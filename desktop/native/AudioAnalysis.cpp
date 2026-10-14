#define NOMINMAX
#include "AudioAnalysis.h"
#include "CodecReader.h"
#include "Features.h"
#include <mfapi.h>
#include <mferror.h>
#include <mfidl.h>
#include <mfreadwrite.h>
#include <propvarutil.h>
#include <wrl/client.h>
using Microsoft::WRL::ComPtr;
namespace ga
{
AudioFeatures analyseAudio(const std::wstring &path, const std::function<bool()> &cancel, double seconds)
{
    AudioFeatures result;
    if (!localMediaPath(path))
    {
        result.error = L"Only local files may be analysed.";
        return result;
    }
    struct Apartment
    {
        HRESULT hr = CoInitializeEx(nullptr, COINIT_MULTITHREADED);
        ~Apartment()
        {
            if (SUCCEEDED(hr))
                CoUninitialize();
        }
    } apartment;
    if (FAILED(apartment.hr))
    {
        result.error = L"Audio analysis could not initialize.";
        return result;
    }
    {
        ComPtr<IMFSourceReader> reader;
        ComPtr<IMFAttributes> attributes;
        ComPtr<IMFMediaType> type;
        HRESULT hr = MFCreateAttributes(&attributes, 2);
        if (SUCCEEDED(hr))
            hr = attributes->SetUINT32(MF_READWRITE_ENABLE_HARDWARE_TRANSFORMS, TRUE);
        if (SUCCEEDED(hr))
            hr = MFCreateSourceReaderFromURL(path.c_str(), attributes.Get(), &reader);
        if (SUCCEEDED(hr))
            hr = reader->SetStreamSelection(static_cast<DWORD>(MF_SOURCE_READER_ALL_STREAMS), FALSE);
        if (SUCCEEDED(hr))
            hr = reader->SetStreamSelection(static_cast<DWORD>(MF_SOURCE_READER_FIRST_AUDIO_STREAM), TRUE);
        if (SUCCEEDED(hr))
            hr = MFCreateMediaType(&type);
        if (SUCCEEDED(hr))
        {
            type->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Audio);
            type->SetGUID(MF_MT_SUBTYPE, MFAudioFormat_Float);
            hr = reader->SetCurrentMediaType(static_cast<DWORD>(MF_SOURCE_READER_FIRST_AUDIO_STREAM), nullptr,
                                             type.Get());
        }
        if (FAILED(hr))
        {
            result.error = L"Windows cannot decode this audio for analysis. HRESULT=" + std::to_wstring(hr);
        }
        else
        {
            PROPVARIANT duration;
            PropVariantInit(&duration);
            if (SUCCEEDED(reader->GetPresentationAttribute(static_cast<DWORD>(MF_SOURCE_READER_MEDIASOURCE),
                                                           MF_PD_DURATION, &duration)) &&
                duration.vt == VT_UI8)
                result.duration = duration.uhVal.QuadPart / 10000000.0;
            PropVariantClear(&duration);
            ComPtr<IMFMediaType> current;
            reader->GetCurrentMediaType(static_cast<DWORD>(MF_SOURCE_READER_FIRST_AUDIO_STREAM), &current);
            UINT32 rate = 48000, channels = 1;
            if (current)
            {
                current->GetUINT32(MF_MT_AUDIO_SAMPLES_PER_SECOND, &rate);
                current->GetUINT32(MF_MT_AUDIO_NUM_CHANNELS, &channels);
            }
            if (rate < 8000 || rate > 768000 || channels < 1 || channels > 128)
            {
                result.error = L"Unsupported analysis audio layout.";
                return result;
            }
            double sum = 0;
            int count = 0;
            std::uint64_t decoded = 0;
            float previous = 0;
            auto maximum = std::uint64_t(std::clamp(seconds, 1.0, 43200.0) * rate);
            int hop = int(rate / 50);
            while (!cancel() && decoded < maximum)
            {
                DWORD flags = 0;
                LONGLONG stamp = 0;
                ComPtr<IMFSample> sample;
                hr = reader->ReadSample(static_cast<DWORD>(MF_SOURCE_READER_FIRST_AUDIO_STREAM), 0, nullptr, &flags,
                                        &stamp, &sample);
                if (FAILED(hr))
                {
                    result.error = L"Audio decoding failed during analysis.";
                    break;
                }
                if (flags & MF_SOURCE_READERF_ENDOFSTREAM)
                    break;
                if (flags & MF_SOURCE_READERF_CURRENTMEDIATYPECHANGED)
                {
                    result.error = L"Audio format changed during analysis.";
                    break;
                }
                if (!sample)
                    continue;
                ComPtr<IMFMediaBuffer> buffer;
                if (FAILED(sample->ConvertToContiguousBuffer(&buffer)))
                    continue;
                BYTE *data = nullptr;
                DWORD size = 0;
                if (FAILED(buffer->Lock(&data, nullptr, &size)))
                    continue;
                auto pcm = reinterpret_cast<const float *>(data);
                for (DWORD i = 0; i + channels <= size / 4 && decoded < maximum; i += channels, ++decoded)
                {
                    double energy = 0;
                    for (UINT32 channel = 0; channel < channels; ++channel)
                    {
                        double value = std::isfinite(pcm[i + channel]) ? pcm[i + channel] : 0;
                        energy += value * value;
                    }
                    sum += energy / channels;
                    if (++count == hop)
                    {
                        float rms = float(std::sqrt(sum / count));
                        result.energy.push_back(rms);
                        result.attacks.push_back((std::max)(0.0f, rms - previous));
                        previous = rms;
                        sum = 0;
                        count = 0;
                    }
                }
                buffer->Unlock();
            }
            if (cancel())
            {
                result.energy.clear();
                result.attacks.clear();
                result.error = L"Analysis cancelled.";
            }
        }
    }
    if (!result.error.empty() && !cancel() && codecsAvailable())
    {
        CodecReader reader;
        if (reader.open(path, cancel))
        {
            result = {};
            auto info = reader.info();
            result.duration = info.duration;
            std::vector<float> pcm;
            double timestamp = 0, sum = 0;
            int count = 0;
            float previous = 0;
            int hop = info.rate / 50;
            while (!cancel() && reader.read(pcm, timestamp) && timestamp < seconds)
            {
                for (std::size_t i = 0; i + info.channels <= pcm.size(); i += info.channels)
                {
                    double energy = 0;
                    for (int c = 0; c < info.channels; ++c)
                    {
                        double value = std::isfinite(pcm[i + c]) ? pcm[i + c] : 0;
                        energy += value * value;
                    }
                    sum += energy / info.channels;
                    if (++count == hop)
                    {
                        float rms = float(std::sqrt(sum / count));
                        result.energy.push_back(rms);
                        result.attacks.push_back(std::max(0.0f, rms - previous));
                        previous = rms;
                        sum = 0;
                        count = 0;
                    }
                }
            }
            result.error = reader.info().error;
        }
    }
    return result;
}
bool audioDecodeTest(const std::wstring &directory)
{
    auto root = std::filesystem::path(directory);
    auto primary = root / L"analysis-primary.wav", delayed = root / L"analysis-delayed.wav";
    auto write = [&](const std::filesystem::path &path, int delay) {
        std::ofstream out(path, std::ios::binary);
        int samples = 48000 * 8;
        std::uint32_t data = samples * 2, riff = data + 36, rate = 48000, byteRate = 96000, fmt = 16;
        std::uint16_t type = 1, channels = 1, align = 2, bits = 16;
        out.write("RIFF", 4);
        out.write(reinterpret_cast<char *>(&riff), 4);
        out.write("WAVEfmt ", 8);
        out.write(reinterpret_cast<char *>(&fmt), 4);
        out.write(reinterpret_cast<char *>(&type), 2);
        out.write(reinterpret_cast<char *>(&channels), 2);
        out.write(reinterpret_cast<char *>(&rate), 4);
        out.write(reinterpret_cast<char *>(&byteRate), 4);
        out.write(reinterpret_cast<char *>(&align), 2);
        out.write(reinterpret_cast<char *>(&bits), 2);
        out.write("data", 4);
        out.write(reinterpret_cast<char *>(&data), 4);
        for (int i = 0; i < samples; ++i)
        {
            double t = double(i - delay) / 48000;
            double envelope = std::exp(-std::pow((t - .7) / .04, 2)) + .7 * std::exp(-std::pow((t - 2.3) / .07, 2)) +
                              .9 * std::exp(-std::pow((t - 5.1) / .05, 2)) +
                              .6 * std::exp(-std::pow((t - 6.7) / .08, 2));
            std::int16_t sample = std::int16_t(envelope * 12000 * std::sin(t * 440 * 6.283185307));
            out.write(reinterpret_cast<char *>(&sample), 2);
        }
        return bool(out);
    };
    if (!write(primary, 0) || !write(delayed, 24000) || FAILED(MFStartup(MF_VERSION)))
        return false;
    auto a = analyseAudio(
             primary.wstring(), [] { return false; }, 8),
         b = analyseAudio(delayed.wstring(), [] { return false; }, 8);
    auto match = matchAudio(a, b);
    std::ofstream report(root / L"audio-decoder-test.txt");
    report << "a=" << a.energy.size() << " b=" << b.energy.size() << " offset=" << match.offset
           << " confidence=" << match.confidence << " accepted=" << match.accepted << " errorA=" << toUtf8(a.error)
           << " errorB=" << toUtf8(b.error) << "\n";
    MFShutdown();
    return a.error.empty() && b.error.empty() && match.accepted && std::abs(match.offset - .5) < .03;
}
} // namespace ga
