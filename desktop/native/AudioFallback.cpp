#define NOMINMAX
#include "AudioFallback.h"
#include "CodecReader.h"
#include <algorithm>
#include <atomic>
#include <audioclient.h>
#include <avrt.h>
#include <cmath>
#include <deque>
#include <ksmedia.h>
#include <mmdeviceapi.h>
#include <mutex>
#include <thread>
#include <wrl/client.h>
#ifdef GA_HAS_CODECS
extern "C"
{
#include <libavfilter/avfilter.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavutil/channel_layout.h>
}
using Microsoft::WRL::ComPtr;
namespace ga
{
struct AudioFallback::Impl
{
    std::atomic<bool> closing{false}, paused{true};
    std::atomic<bool> looping{false};
    std::atomic<double> gain{1}, requestedRate{1};
    std::mutex mutex;
    double pendingSeek = -1;
    std::vector<PcmVoice> voiceConfiguration;
    std::uint64_t voiceRevision = 1;
    MediaState snapshot;
    std::thread worker;
    AVFilterGraph *graph = nullptr;
    AVFilterContext *source = nullptr, *tempo = nullptr, *sink = nullptr;
    ~Impl()
    {
        closing = true;
        if (worker.joinable())
            worker.join();
        avfilter_graph_free(&graph);
    }
    bool filter(int rate, int channels)
    {
        avfilter_graph_free(&graph);
        graph = avfilter_graph_alloc();
        if (!graph)
            return false;
        graph->nb_threads = 1;
        AVChannelLayout layout{};
        av_channel_layout_default(&layout, channels);
        char name[128]{};
        av_channel_layout_describe(&layout, name, sizeof(name));
        av_channel_layout_uninit(&layout);
        char arguments[256]{};
        sprintf_s(arguments, "time_base=1/%d:sample_rate=%d:sample_fmt=flt:channel_layout=%s", rate, rate,
                  name);
        if (avfilter_graph_create_filter(&source, avfilter_get_by_name("abuffer"), "source", arguments,
                                         nullptr, graph) < 0 ||
            avfilter_graph_create_filter(&tempo, avfilter_get_by_name("atempo"), "tempo", "tempo=1", nullptr,
                                         graph) < 0 ||
            avfilter_graph_create_filter(&sink, avfilter_get_by_name("abuffersink"), "sink", nullptr, nullptr,
                                         graph) < 0)
            return false;
        return avfilter_link(source, 0, tempo, 0) >= 0 && avfilter_link(tempo, 0, sink, 0) >= 0 &&
               avfilter_graph_config(graph, nullptr) >= 0;
    }
    void update(int state, double time, double duration)
    {
        std::lock_guard<std::mutex> lock(mutex);
        snapshot.state = state;
        snapshot.time = snapshot.sharedClock && looping && duration > 0 ? std::fmod(time, duration) : time;
        snapshot.duration = duration;
    }
    void run(std::wstring path, bool audioOnly)
    {
        struct Apartment
        {
            HRESULT hr = CoInitializeEx(nullptr, COINIT_MULTITHREADED);
            ~Apartment()
            {
                if (SUCCEEDED(hr))
                    CoUninitialize();
            }
        };
        Apartment apartment;
        if (FAILED(apartment.hr))
        {
            update(7, 0, 0);
            return;
        }
        {
            struct AudioScheduling
            {
                DWORD task = 0;
                HANDLE handle = AvSetMmThreadCharacteristicsW(L"Audio", &task);
                ~AudioScheduling()
                {
                    if (handle)
                        AvRevertMmThreadCharacteristics(handle);
                }
            } scheduling;
            struct ReadyEvent
            {
                HANDLE handle = CreateEventW(nullptr, FALSE, FALSE, nullptr);
                ~ReadyEvent()
                {
                    if (handle)
                        CloseHandle(handle);
                }
            } ready;
            if (!ready.handle)
            {
                update(7, 0, 0);
                return;
            }
            ComPtr<IMMDeviceEnumerator> devices;
            ComPtr<IMMDevice> device;
            ComPtr<IAudioClient> client;
            ComPtr<IAudioRenderClient> renderer;
            HRESULT result = CoCreateInstance(__uuidof(MMDeviceEnumerator), nullptr, CLSCTX_INPROC_SERVER,
                                              IID_PPV_ARGS(&devices));
            if (SUCCEEDED(result))
                result = devices->GetDefaultAudioEndpoint(eRender, eMultimedia, &device);
            if (SUCCEEDED(result))
                result = device->Activate(__uuidof(IAudioClient), CLSCTX_INPROC_SERVER, nullptr,
                                          reinterpret_cast<void **>(client.GetAddressOf()));
            WAVEFORMATEX *format = nullptr;
            if (SUCCEEDED(result))
                result = client->GetMixFormat(&format);
            if (FAILED(result) || !format)
            {
                update(7, 0, 0);
                return;
            }
            bool floating = format->wFormatTag == WAVE_FORMAT_IEEE_FLOAT ||
                            (format->wFormatTag == WAVE_FORMAT_EXTENSIBLE &&
                             reinterpret_cast<WAVEFORMATEXTENSIBLE *>(format)->SubFormat ==
                                 KSDATAFORMAT_SUBTYPE_IEEE_FLOAT);
            int sampleRate = int(format->nSamplesPerSec), channels = int(format->nChannels),
                bits = format->wBitsPerSample;
            {
                std::lock_guard<std::mutex> lock(mutex);
                snapshot.pcmRate = sampleRate;
                snapshot.pcmChannels = channels;
                snapshot.sharedClock = !voiceConfiguration.empty();
            }
            if (sampleRate < 8000 || sampleRate > 768000 || channels < 1 || channels > 32 ||
                (floating && bits != 32) || (!floating && bits != 16 && bits != 24 && bits != 32) ||
                format->nBlockAlign != channels * bits / 8)
            {
                CoTaskMemFree(format);
                update(7, 0, 0);
                return;
            }
            result = client->Initialize(AUDCLNT_SHAREMODE_SHARED, AUDCLNT_STREAMFLAGS_EVENTCALLBACK, 1000000,
                                        0, format, nullptr);
            CoTaskMemFree(format);
            if (SUCCEEDED(result))
                result = client->SetEventHandle(ready.handle);
            UINT32 capacity = 0;
            if (SUCCEEDED(result))
                result = client->GetBufferSize(&capacity);
            if (SUCCEEDED(result))
                result = client->GetService(IID_PPV_ARGS(&renderer));
            CodecReader decoder;
            std::vector<PcmVoice> voices;
            std::uint64_t revision = 0;
            {
                std::lock_guard<std::mutex> lock(mutex);
                voices = voiceConfiguration;
                revision = voiceRevision;
            }
            std::unique_ptr<StackPcm> stack;
            if (FAILED(result) || !capacity)
            {
                update(7, 0, 0);
                return;
            }
            CodecInfo metadata;
            if (!voices.empty())
            {
                stack = std::make_unique<StackPcm>(voices, sampleRate, channels);
                stack->loop(looping);
                update(1, 0, 0);
                while (!closing)
                {
                    auto sourceState = stack->state();
                    if (!sourceState.error.empty())
                    {
                        update(7, 0, 0);
                        return;
                    }
                    if (sourceState.ready)
                    {
                        metadata = sourceState.primary;
                        break;
                    }
                    Sleep(3);
                }
                if (closing)
                    return;
            }
            else
            {
                if (!decoder.open(path, [&] { return closing.load(); }, sampleRate, channels))
                {
                    update(7, 0, 0);
                    return;
                }
                metadata = decoder.info();
            }
            if (!stack && !audioOnly && metadata.width)
            {
                update(7, 0, metadata.duration);
                return;
            }
            if (!filter(sampleRate, channels))
            {
                update(7, 0, metadata.duration);
                return;
            }
            struct FrameOwner
            {
                AVFrame *value = av_frame_alloc();
                ~FrameOwner()
                {
                    av_frame_free(&value);
                }
            } inputOwner, outputOwner;
            AVFrame *input = inputOwner.value, *output = outputOwner.value;
            if (!input || !output)
            {
                update(7, 0, metadata.duration);
                return;
            }
            struct Chunk
            {
                std::vector<float> pcm;
                std::size_t head = 0;
                double rate = 1;
            };
            struct Span
            {
                std::uint64_t end;
                double time, rate;
            };
            std::deque<Chunk> queued;
            std::deque<Span> spans;
            std::size_t queuedSamples = 0;
            double origin = 0, rate = 1, timeline = 0, seekStart = 0;
            std::uint64_t submitted = 0, sourceSamples = 0;
            bool ended = false, started = false, flushed = false;
            bool emptyEndpoint = false;
            update(4, 0, metadata.duration);
            while (!closing)
            {
                double seek = -1;
                bool voicesChanged = false;
                {
                    std::lock_guard<std::mutex> lock(mutex);
                    seek = pendingSeek;
                    pendingSeek = -1;
                    if (revision != voiceRevision)
                    {
                        voices = voiceConfiguration;
                        revision = voiceRevision;
                        voicesChanged = true;
                    }
                }
                if (stack && voicesChanged && seek < 0 && !stack->voices(voices))
                {
                    update(7, timeline, metadata.duration);
                    break;
                }
                if (stack)
                {
                    stack->loop(looping);
                    auto duration = stack->state().primary.duration;
                    if (duration > 0)
                        metadata.duration = duration;
                }
                if (seek >= 0)
                {
                    if (started)
                        client->Stop();
                    result = client->Reset();
                    started = false;
                    queued.clear();
                    spans.clear();
                    queuedSamples = 0;
                    submitted = sourceSamples = 0;
                    origin = timeline = seekStart = seek;
                    ended = flushed = false;
                    bool reset = stack ? stack->seek(seek, voices) : decoder.seek(seek);
                    if (FAILED(result) || !reset || !filter(sampleRate, channels))
                    {
                        update(7, seek, metadata.duration);
                        break;
                    }
                    rate = 1;
                }
                double nextRate = requestedRate;
                if (nextRate != rate)
                {
                    auto text = std::to_string(nextRate);
                    if (avfilter_graph_send_command(graph, "tempo", "tempo", text.c_str(), nullptr, 0, 0) < 0)
                    {
                        update(7, timeline, metadata.duration);
                        break;
                    }
                    rate = nextRate;
                }
                UINT32 padding = 0;
                if (FAILED(client->GetCurrentPadding(&padding)))
                {
                    update(7, origin, metadata.duration);
                    break;
                }
                auto consumed = submitted >= padding ? submitted - padding : 0;
                while (!spans.empty() && consumed >= spans.front().end)
                {
                    origin = spans.front().time;
                    spans.pop_front();
                }
                double time = spans.empty() ? origin
                                            : spans.front().time - double(spans.front().end - consumed) /
                                                                       sampleRate * spans.front().rate;
                if (paused)
                {
                    if (started)
                    {
                        client->Stop();
                        started = false;
                    }
                    update(stack && !stack->state().ready ? 1 : 4, time, metadata.duration);
                    Sleep(5);
                    continue;
                }
                if (stack && started && !padding && !ended && !emptyEndpoint)
                {
                    std::lock_guard<std::mutex> lock(mutex);
                    ++snapshot.pcmUnderruns;
                }
                emptyEndpoint = !padding;
                if (queuedSamples < std::size_t(capacity) * channels && !ended)
                {
                    std::vector<float> pcm;
                    double timestamp = 0;
                    PcmRead read = stack ? stack->read(pcm, timestamp)
                                         : (decoder.read(pcm, timestamp) ? PcmRead::Data : PcmRead::End);
                    if (read == PcmRead::Data)
                    {
                        std::size_t skip = !stack && timestamp < seekStart
                                               ? std::size_t((seekStart - timestamp) * sampleRate) * channels
                                               : 0;
                        if (skip < pcm.size())
                        {
                            av_frame_unref(input);
                            input->format = AV_SAMPLE_FMT_FLT;
                            input->sample_rate = sampleRate;
                            av_channel_layout_default(&input->ch_layout, channels);
                            input->nb_samples = int((pcm.size() - skip) / channels);
                            input->pts = std::int64_t(sourceSamples);
                            if (av_frame_get_buffer(input, 0) < 0)
                            {
                                update(7, time, metadata.duration);
                                break;
                            }
                            memcpy(input->data[0], pcm.data() + skip, (pcm.size() - skip) * sizeof(float));
                            sourceSamples += input->nb_samples;
                            if (av_buffersrc_add_frame_flags(source, input, AV_BUFFERSRC_FLAG_KEEP_REF) < 0)
                            {
                                update(7, time, metadata.duration);
                                break;
                            }
                        }
                    }
                    else if (read == PcmRead::Error || read == PcmRead::End)
                    {
                        if (read == PcmRead::Error || (!stack && !decoder.info().error.empty()))
                        {
                            update(7, time, metadata.duration);
                            break;
                        }
                        ended = true;
                        if (av_buffersrc_add_frame_flags(source, nullptr, 0) < 0)
                        {
                            update(7, time, metadata.duration);
                            break;
                        }
                        flushed = true;
                    }
                }
                bool invalid = false;
                while (av_buffersink_get_frame(sink, output) >= 0)
                {
                    if (output->format != AV_SAMPLE_FMT_FLT || output->ch_layout.nb_channels != channels)
                    {
                        invalid = true;
                        av_frame_unref(output);
                        break;
                    }
                    auto values = reinterpret_cast<float *>(output->data[0]);
                    auto size = std::size_t(output->nb_samples) * channels;
                    queued.push_back({std::vector<float>(values, values + size), 0, rate});
                    queuedSamples += size;
                    av_frame_unref(output);
                }
                if (invalid || queuedSamples > std::size_t(sampleRate) * channels * 10 || padding > capacity)
                {
                    update(7, time, metadata.duration);
                    break;
                }
                UINT32 available = capacity - padding,
                       frames = queued.empty()
                                    ? 0
                                    : UINT32(std::min(std::size_t(available),
                                                      (queued.front().pcm.size() - queued.front().head) /
                                                          channels));
                if (frames)
                {
                    BYTE *buffer = nullptr;
                    if (FAILED(renderer->GetBuffer(frames, &buffer)))
                    {
                        update(7, time, metadata.duration);
                        break;
                    }
                    double volume = gain;
                    auto &chunk = queued.front();
                    for (std::size_t i = 0; i < std::size_t(frames) * channels; ++i)
                    {
                        double value = double(chunk.pcm[chunk.head + i]) * volume;
                        if (!std::isfinite(value))
                            value = 0;
                        if (floating)
                            reinterpret_cast<float *>(buffer)[i] = float(value);
                        else
                        {
                            value = std::clamp(value, -1.0, 1.0);
                            std::int64_t sample = std::int64_t(value * ((std::int64_t(1) << (bits - 1)) - 1));
                            for (int b = 0; b < bits / 8; ++b)
                                buffer[i * (bits / 8) + b] = BYTE(sample >> (8 * b));
                        }
                    }
                    if (FAILED(renderer->ReleaseBuffer(frames, 0)))
                    {
                        update(7, time, metadata.duration);
                        break;
                    }
                    submitted += frames;
                    timeline += double(frames) / sampleRate * chunk.rate;
                    // Account for already-buffered audio at its own rate; changing speed
                    // must not multiply the whole elapsed timeline by the new speed.
                    if (!spans.empty() && spans.back().rate == chunk.rate)
                    {
                        spans.back().end = submitted;
                        spans.back().time = timeline;
                    }
                    else
                        spans.push_back({submitted, timeline, chunk.rate});
                    chunk.head += std::size_t(frames) * channels;
                    queuedSamples -= std::size_t(frames) * channels;
                    if (chunk.head == chunk.pcm.size())
                        queued.pop_front();
                    if (!started && (padding + frames >= capacity * 3 / 4 || (ended && queued.empty())))
                    {
                        if (FAILED(client->Start()))
                        {
                            update(7, time, metadata.duration);
                            break;
                        }
                        started = true;
                    }
                }
                if (ended && flushed && queued.empty() && !padding)
                {
                    if (started)
                    {
                        client->Stop();
                        started = false;
                    }
                    update(6, metadata.duration, metadata.duration);
                    Sleep(5);
                    continue;
                }
                update(started ? 3 : 1, time, metadata.duration);
                // Small mixed blocks at 2x/4x need more than one admission per
                // timer tick. Fill ahead while data is ready, then follow the
                // endpoint event. Sleep(3) may actually take a 15.6ms tick.
                if (stack && queuedSamples < std::size_t(capacity) * channels && !ended &&
                    stack->state().queuedFrames > 0)
                    continue;
                WaitForSingleObject(ready.handle, 10);
            }
            if (started)
                client->Stop();
        }
    }
};
AudioFallback::AudioFallback(const std::wstring &path, bool audioOnly) : impl(std::make_unique<Impl>())
{
    impl->worker = std::thread([this, path, audioOnly] {
        try
        {
            impl->run(path, audioOnly);
        }
        catch (...)
        {
            impl->update(7, 0, 0);
        }
    });
}
AudioFallback::AudioFallback(std::vector<PcmVoice> voices) : impl(std::make_unique<Impl>())
{
    impl->voiceConfiguration = std::move(voices);
    impl->worker = std::thread([this] {
        try
        {
            impl->run({}, true);
        }
        catch (...)
        {
            impl->update(7, 0, 0);
        }
    });
}
AudioFallback::~AudioFallback() = default;
HRESULT AudioFallback::pause(bool paused)
{
    impl->paused = paused;
    return S_OK;
}
HRESULT AudioFallback::seek(double seconds)
{
    if (!std::isfinite(seconds) || seconds < 0)
        return E_INVALIDARG;
    std::lock_guard<std::mutex> lock(impl->mutex);
    impl->pendingSeek = seconds;
    return S_OK;
}
HRESULT AudioFallback::rate(double rate)
{
    if (!std::isfinite(rate) || rate < .5 || rate > 4)
        return E_INVALIDARG;
    impl->requestedRate = rate;
    return S_OK;
}
HRESULT AudioFallback::volume(double gain)
{
    if (!std::isfinite(gain))
        return E_INVALIDARG;
    impl->gain = std::clamp(gain, 0.0, 1.0);
    return S_OK;
}
HRESULT AudioFallback::voices(const std::vector<PcmVoice> &voices)
{
    std::lock_guard<std::mutex> lock(impl->mutex);
    if (voices.size() != impl->voiceConfiguration.size() || voices.empty())
        return E_INVALIDARG;
    bool offsetChanged = false;
    for (std::size_t i = 0; i < voices.size(); ++i)
    {
        if (voices[i].path != impl->voiceConfiguration[i].path || !std::isfinite(voices[i].gain) ||
            voices[i].gain < 0 || voices[i].gain > 1 || !std::isfinite(voices[i].offset) ||
            std::abs(voices[i].offset) > 30 || (i == 0 && voices[i].offset != 0))
            return E_INVALIDARG;
        offsetChanged = offsetChanged || voices[i].offset != impl->voiceConfiguration[i].offset;
    }
    impl->voiceConfiguration = voices;
    ++impl->voiceRevision;
    if (offsetChanged)
        impl->pendingSeek = impl->snapshot.time;
    return S_OK;
}
void AudioFallback::loop(bool enabled)
{
    impl->looping = enabled;
}
MediaState AudioFallback::state() const
{
    std::lock_guard<std::mutex> lock(impl->mutex);
    return impl->snapshot;
}
} // namespace ga
#else
namespace ga
{
struct AudioFallback::Impl
{
};
AudioFallback::AudioFallback(const std::wstring &, bool) : impl(std::make_unique<Impl>())
{
}
AudioFallback::AudioFallback(std::vector<PcmVoice>) : impl(std::make_unique<Impl>())
{
}
AudioFallback::~AudioFallback() = default;
HRESULT AudioFallback::pause(bool)
{
    return E_NOTIMPL;
}
HRESULT AudioFallback::seek(double)
{
    return E_NOTIMPL;
}
HRESULT AudioFallback::rate(double)
{
    return E_NOTIMPL;
}
HRESULT AudioFallback::volume(double)
{
    return E_NOTIMPL;
}
HRESULT AudioFallback::voices(const std::vector<PcmVoice> &)
{
    return E_NOTIMPL;
}
void AudioFallback::loop(bool)
{
}
MediaState AudioFallback::state() const
{
    MediaState state;
    state.state = 7;
    return state;
}
} // namespace ga
#endif
