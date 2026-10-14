#define NOMINMAX
#include "AudioFallback.h"
#include <algorithm>
#include <atomic>
#include <audioclient.h>
#include <audioclientactivationparams.h>
#include <cmath>
#include <filesystem>
#include <fstream>
#include <mmdeviceapi.h>
#include <wrl.h>

namespace ga
{
namespace
{
using namespace Microsoft::WRL;
class Activation final
    : public RuntimeClass<RuntimeClassFlags<ClassicCom>, IActivateAudioInterfaceCompletionHandler, FtmBase>
{
  public:
    HANDLE ready = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    ComPtr<IAudioClient> client;
    HRESULT result = E_PENDING;
    ~Activation()
    {
        if (ready)
            CloseHandle(ready);
    }
    HRESULT STDMETHODCALLTYPE ActivateCompleted(IActivateAudioInterfaceAsyncOperation *operation) override
    {
        ComPtr<IUnknown> unknown;
        HRESULT activation = E_FAIL;
        result = operation->GetActivateResult(&activation, &unknown);
        if (SUCCEEDED(result))
            result = activation;
        if (SUCCEEDED(result))
            result = unknown.As(&client);
        SetEvent(ready);
        return S_OK;
    }
};
struct OwnProcessCapture
{
    ComPtr<IAudioClient> client;
    ComPtr<IAudioCaptureClient> capture;
    HANDLE ready = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    std::vector<float> samples;
    unsigned discontinuities = 0;
    ~OwnProcessCapture()
    {
        if (client)
            client->Stop();
        if (ready)
            CloseHandle(ready);
    }
    HRESULT start()
    {
        if (!ready)
            return HRESULT_FROM_WIN32(GetLastError());
        auto callback = Make<Activation>();
        if (!callback || !callback->ready)
            return E_OUTOFMEMORY;
        AUDIOCLIENT_ACTIVATION_PARAMS parameters{};
        parameters.ActivationType = AUDIOCLIENT_ACTIVATION_TYPE_PROCESS_LOOPBACK;
        parameters.ProcessLoopbackParams.TargetProcessId = GetCurrentProcessId();
        parameters.ProcessLoopbackParams.ProcessLoopbackMode =
            PROCESS_LOOPBACK_MODE_INCLUDE_TARGET_PROCESS_TREE;
        PROPVARIANT variant{};
        variant.vt = VT_BLOB;
        variant.blob.cbSize = sizeof(parameters);
        variant.blob.pBlobData = reinterpret_cast<BYTE *>(&parameters);
        ComPtr<IActivateAudioInterfaceAsyncOperation> operation;
        HRESULT result =
            ActivateAudioInterfaceAsync(VIRTUAL_AUDIO_DEVICE_PROCESS_LOOPBACK, __uuidof(IAudioClient),
                                        &variant, callback.Get(), &operation);
        if (FAILED(result))
            return result;
        if (WaitForSingleObject(callback->ready, 15000) != WAIT_OBJECT_0)
            return HRESULT_FROM_WIN32(WAIT_TIMEOUT);
        if (FAILED(callback->result))
            return callback->result;
        client = callback->client;
        WAVEFORMATEX format{};
        format.wFormatTag = WAVE_FORMAT_IEEE_FLOAT;
        format.nChannels = 2;
        format.nSamplesPerSec = 48000;
        format.wBitsPerSample = 32;
        format.nBlockAlign = 8;
        format.nAvgBytesPerSec = 384000;
        result = client->Initialize(AUDCLNT_SHAREMODE_SHARED,
                                    AUDCLNT_STREAMFLAGS_LOOPBACK | AUDCLNT_STREAMFLAGS_EVENTCALLBACK |
                                        AUDCLNT_STREAMFLAGS_AUTOCONVERTPCM |
                                        AUDCLNT_STREAMFLAGS_SRC_DEFAULT_QUALITY,
                                    0, 0, &format, nullptr);
        if (SUCCEEDED(result))
            result = client->GetService(IID_PPV_ARGS(&capture));
        if (SUCCEEDED(result))
            result = client->SetEventHandle(ready);
        if (SUCCEEDED(result))
            result = client->Start();
        return result;
    }
    HRESULT collect()
    {
        UINT32 packet = 0;
        HRESULT result = capture->GetNextPacketSize(&packet);
        while (SUCCEEDED(result) && packet)
        {
            BYTE *data = nullptr;
            UINT32 frames = 0;
            DWORD flags = 0;
            result = capture->GetBuffer(&data, &frames, &flags, nullptr, nullptr);
            if (FAILED(result))
                return result;
            if (flags & AUDCLNT_BUFFERFLAGS_DATA_DISCONTINUITY)
                ++discontinuities;
            for (UINT32 frame = 0; frame < frames; ++frame)
                samples.push_back(
                    flags & AUDCLNT_BUFFERFLAGS_SILENT ? 0 : reinterpret_cast<float *>(data)[frame * 2]);
            result = capture->ReleaseBuffer(frames);
            if (samples.size() > 48000 * 10)
                return E_OUTOFMEMORY;
            if (SUCCEEDED(result))
                result = capture->GetNextPacketSize(&packet);
        }
        return result;
    }
};
void wave(const std::filesystem::path &path, const std::vector<float> &pcm)
{
    std::ofstream out(path, std::ios::binary);
    std::uint32_t length = std::uint32_t(pcm.size() * 4), size = length + 36, fmt = 16, rate = 48000,
                  bytes = 192000;
    std::uint16_t type = 3, channels = 1, align = 4, bits = 32;
    auto write = [&](auto value) { out.write(reinterpret_cast<const char *>(&value), sizeof(value)); };
    out.write("RIFF", 4);
    write(size);
    out.write("WAVEfmt ", 8);
    write(fmt);
    write(type);
    write(channels);
    write(rate);
    write(bytes);
    write(align);
    write(bits);
    out.write("data", 4);
    write(length);
    out.write(reinterpret_cast<const char *>(pcm.data()), length);
}
HRESULT record(const std::vector<PcmVoice> &voices, std::vector<float> &output, unsigned &underruns,
               const std::filesystem::path &destination)
{
    OwnProcessCapture recorder;
    HRESULT result = recorder.start();
    if (FAILED(result))
        return result;
    AudioFallback player(voices);
    player.volume(.15); // Quiet owned noise fixture, not a user's media setting.
    auto deadline = GetTickCount64() + 7000;
    while (player.state().state != 4 && GetTickCount64() < deadline)
    {
        if (player.state().state == 7)
            return E_FAIL;
        recorder.collect();
        Sleep(5);
    }
    if (player.state().state != 4)
        return HRESULT_FROM_WIN32(WAIT_TIMEOUT);
    recorder.samples.clear();
    player.pause(false);
    deadline = GetTickCount64() + 4000;
    while (GetTickCount64() < deadline)
    {
        WaitForSingleObject(recorder.ready, 20);
        result = recorder.collect();
        if (FAILED(result) || player.state().state == 7)
            return FAILED(result) ? result : E_FAIL;
    }
    player.pause(true);
    underruns = unsigned(player.state().pcmUnderruns);
    output = std::move(recorder.samples);
    wave(destination, output);
    return S_OK;
}
double difference(const std::vector<float> &reference, const std::vector<float> &actual, int &lag)
{
    auto onset = [](const auto &samples) {
        auto found =
            std::find_if(samples.begin(), samples.end(), [](float value) { return std::abs(value) > .0001; });
        return int(found - samples.begin());
    };
    int first = onset(reference), second = onset(actual);
    if (first + 72000 >= int(reference.size()) || second + 72000 >= int(actual.size()))
        return 1;
    double best = 1;
    lag = 0;
    for (int shift = second - first - 128; shift <= second - first + 128; ++shift)
    {
        double aa = 0, bb = 0, ab = 0;
        for (int i = first + 1024; i < first + 72000; i += 4)
        {
            if (i + shift < 0 || i + shift >= int(actual.size()))
                continue;
            double a = reference[i], b = actual[i + shift];
            aa += a * a;
            bb += b * b;
            ab += a * b;
        }
        double error = aa > 1e-10 && bb > 1e-10 ? std::sqrt(std::max(0.0, 1 - ab * ab / (aa * bb))) : 1;
        if (error < best)
        {
            best = error;
            lag = shift;
        }
    }
    return best;
}
} // namespace
bool stackOutputTests(const std::wstring &directory)
{
    std::filesystem::create_directories(directory);
    auto root = std::filesystem::path(directory);
    std::ofstream report(root / L"stack-output-tests.txt");
    HRESULT com = CoInitializeEx(nullptr, COINIT_MULTITHREADED);
    struct Apartment
    {
        HRESULT result;
        ~Apartment()
        {
            if (SUCCEEDED(result))
                CoUninitialize();
        }
    } apartment{com};
    if (FAILED(com))
    {
        report << "FAIL COM initialization\n";
        return false;
    }
    constexpr int count = 144000;
    std::vector<float> mixed(count, 0);
    std::vector<PcmVoice> voices;
    for (int voice = 0; voice < 6; ++voice)
    {
        int delay = voice * 137;
        std::vector<float> signal(count + delay, 0);
        std::uint32_t random = 1257 + voice * 6271;
        for (int frame = 0; frame < count; ++frame)
        {
            random ^= random << 13;
            random ^= random >> 17;
            random ^= random << 5;
            float value = frame > 9600 && frame < 120000 ? float((double(random) / UINT32_MAX - .5) * .5) : 0;
            signal[frame + delay] = value;
            mixed[frame] += value / 6;
        }
        auto path = root / (L"output-voice-" + std::to_wstring(voice) + L".wav");
        wave(path, signal);
        voices.push_back({path.wstring(), 1, double(delay) / 48000});
    }
    // The primary duration is exactly three seconds, like the reference.
    auto referenceFile = root / L"output-calibration.wav";
    wave(referenceFile, mixed);
    std::vector<float> calibration, actual, sabotaged;
    unsigned firstUnderruns = 0, actualUnderruns = 0, badUnderruns = 0;
    HRESULT result = record({{referenceFile.wstring(), 1, 0}}, calibration, firstUnderruns,
                            root / L"captured-calibration.wav");
    if (SUCCEEDED(result))
        result = record(voices, actual, actualUnderruns, root / L"captured-six-voices.wav");
    int lag = 0;
    double error = difference(calibration, actual, lag);
    auto bad = voices;
    bad[3].offset += 1.0 / 48000;
    if (SUCCEEDED(result))
        result = record(bad, sabotaged, badUnderruns, root / L"captured-one-sample-sabotage.wav");
    int badLag = 0;
    double badError = difference(calibration, sabotaged, badLag);
    bool passed = SUCCEEDED(result) && error < .015 && badError > .04 && badError > error * 5 &&
                  firstUnderruns == 0 && actualUnderruns == 0;
    report << (passed ? "PASS" : "FAIL") << " own_process_WASAPI_loopback\nHRESULT=" << std::hex << result
           << std::dec << "\nnormalizedError=" << error << "\nglobalCaptureLagFrames=" << lag
           << "\noneSampleSabotageError=" << badError << "\nunderruns=" << firstUnderruns << ','
           << actualUnderruns
           << "\nother_app_audio_captured=false\nmicrophone_used=false\nphysical_acoustic_proof=false\n";
    return passed;
}
} // namespace ga
