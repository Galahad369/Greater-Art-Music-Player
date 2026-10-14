#define NOMINMAX
#include "StackPcm.h"
#include <algorithm>
#include <array>
#include <atomic>
#include <cmath>
#include <condition_variable>
#include <deque>
#include <fstream>
#include <mutex>
#include <thread>
#include <windows.h>

namespace ga
{
namespace
{
constexpr int BLOCK = 1024, TAPS = 32, QUEUE = 6;
constexpr double PI = 3.14159265358979323846;
bool validVoices(const std::vector<PcmVoice> &voices)
{
    if (voices.empty() || voices.size() > 8)
        return false;
    for (const auto &voice : voices)
        if (voice.path.empty() || !std::isfinite(voice.gain) || voice.gain < 0 || voice.gain > 1 ||
            !std::isfinite(voice.offset) || std::abs(voice.offset) > 30)
            return false;
    return voices[0].offset == 0;
}
struct Segment
{
    std::int64_t start;
    std::vector<float> pcm;
};
struct Input
{
    CodecReader decoder;
    std::deque<Segment> segments;
    std::int64_t end = 0;
    bool ended = false;
    int rate = 0, channels = 0;
    bool ensure(std::int64_t last, const std::atomic<bool> &closing, std::wstring &error)
    {
        while (!ended && end <= last && !closing)
        {
            std::vector<float> pcm;
            double timestamp = 0;
            if (!decoder.read(pcm, timestamp))
            {
                error = decoder.info().error;
                ended = true;
                break;
            }
            if (!std::isfinite(timestamp) || std::abs(timestamp) > 1e9 || pcm.size() % channels)
            {
                error = L"Invalid decoded audio timeline.";
                return false;
            }
            if (pcm.empty())
                continue;
            auto start = std::int64_t(std::llround(timestamp * rate));
            auto next = start + std::int64_t(pcm.size() / channels);
            if (!segments.empty() && start < segments.back().start)
            {
                error = L"Non-monotonic audio timestamps.";
                return false;
            }
            end = std::max(end, next);
            segments.push_back({start, std::move(pcm)});
            // Seeking may land well before the requested frame. Discard old
            // packets as we decode, not after accumulating an entire intro.
            while (segments.size() > 1 &&
                   segments.front().start + std::int64_t(segments.front().pcm.size() / channels) <
                       last - rate)
                segments.pop_front();
        }
        return error.empty() && !closing;
    }
    float at(std::int64_t frame, int channel) const
    {
        if (frame < 0)
            return 0;
        for (const auto &segment : segments)
        {
            if (frame < segment.start)
                return 0; // Genuine timestamp gap, not a fabricated continuation.
            auto index = frame - segment.start;
            if (index < std::int64_t(segment.pcm.size() / channels))
                return segment.pcm[std::size_t(index) * channels + channel];
        }
        return 0;
    }
    void discard(std::int64_t first)
    {
        while (!segments.empty() &&
               segments.front().start + std::int64_t(segments.front().pcm.size() / channels) <= first)
            segments.pop_front();
    }
};
std::array<double, TAPS> kernel(double fraction)
{
    std::array<double, TAPS> result{};
    double sum = 0;
    for (int tap = 0; tap < TAPS; ++tap)
    {
        double x = tap - (TAPS / 2 - 1) - fraction;
        double sinc = std::abs(x) < 1e-12 ? 1 : std::sin(PI * x) / (PI * x);
        // Symmetric raised-cosine window about the interpolation point.
        double window = std::abs(x) < TAPS / 2 ? .5 + .5 * std::cos(PI * x / (TAPS / 2)) : 0;
        result[tap] = sinc * window;
        sum += result[tap];
    }
    if (std::abs(sum) > 1e-12)
        for (auto &value : result)
            value /= sum;
    return result;
}
} // namespace

struct StackPcm::Impl
{
    std::atomic<bool> closing{false};
    mutable std::mutex mutex;
    std::condition_variable wake;
    std::vector<PcmVoice> configuration;
    struct Block
    {
        std::vector<float> pcm;
        double timestamp;
        std::uint64_t generation;
    };
    std::deque<Block> queue;
    PcmStackState snapshot;
    bool ended = false, looping = false;
    double requestedSeek = 0;
    std::uint64_t requestedGeneration = 1;
    int rate, channels;
    std::thread worker;
    Impl(std::vector<PcmVoice> voices, int sampleRate, int count)
        : configuration(std::move(voices)), rate(sampleRate), channels(count)
    {
    }
    ~Impl()
    {
        closing = true;
        wake.notify_all();
        if (worker.joinable())
            worker.join();
    }
    void fail(const std::wstring &error)
    {
        std::lock_guard<std::mutex> lock(mutex);
        snapshot.error = error.empty() ? L"Stack audio decoding failed." : error;
        queue.clear();
        snapshot.queuedFrames = 0;
        wake.notify_all();
    }
    void run()
    {
        std::vector<PcmVoice> initial;
        {
            std::lock_guard<std::mutex> lock(mutex);
            initial = configuration;
        }
        if (!validVoices(initial) || rate < 8000 || rate > 768000 || channels < 1 || channels > 32)
        {
            fail(L"Invalid Stack audio configuration.");
            return;
        }
        std::vector<std::unique_ptr<Input>> inputs;
        for (const auto &voice : initial)
        {
            auto input = std::make_unique<Input>();
            input->rate = rate;
            input->channels = channels;
            if (!input->decoder.open(voice.path, [&] { return closing.load(); }, rate, channels))
            {
                fail(input->decoder.info().error);
                return;
            }
            inputs.push_back(std::move(input));
        }
        auto primary = inputs[0]->decoder.info();
        if (!std::isfinite(primary.duration) || primary.duration <= 0 || primary.duration > 1e9)
        {
            fail(L"Primary audio duration is unavailable.");
            return;
        }
        {
            std::lock_guard<std::mutex> lock(mutex);
            snapshot.primary = primary;
        }
        std::int64_t master = 0, limit = std::int64_t(std::llround(primary.duration * rate));
        std::uint64_t generation = 0;
        std::vector<PcmVoice> voices;
        while (!closing)
        {
            double seek = 0;
            std::uint64_t requested;
            {
                std::unique_lock<std::mutex> lock(mutex);
                wake.wait(lock, [&] {
                    return closing || requestedGeneration != generation || (!ended && queue.size() < QUEUE);
                });
                if (closing)
                    return;
                requested = requestedGeneration;
                seek = requestedSeek;
                voices = configuration;
            }
            if (generation != requested)
            {
                master = std::int64_t(std::llround(std::min(seek, primary.duration) * rate));
                limit = std::int64_t(std::llround(primary.duration * rate));
                for (std::size_t i = 0; i < inputs.size(); ++i)
                {
                    auto &input = *inputs[i];
                    input.segments.clear();
                    input.ended = false;
                    input.end = 0;
                    double target =
                        std::max(0.0, double(master) / rate + voices[i].offset - double(TAPS) / rate);
                    auto voiceDuration = input.decoder.info().duration;
                    if (i > 0 && voiceDuration > 0 && target >= voiceDuration)
                    {
                        input.ended = true;
                        input.end = std::int64_t(std::llround(voiceDuration * rate));
                        continue; // An exhausted companion contributes silence.
                    }
                    if (target > 0 && !input.decoder.seek(target))
                    {
                        fail(L"A Stack voice could not seek to the common timeline.");
                        return;
                    }
                    // A rewind must reset the decoder too, including after EOF.
                    if (target == 0 && generation != 0 && !input.decoder.seek(0))
                    {
                        fail(L"A Stack voice could not restart.");
                        return;
                    }
                }
                generation = requested;
            }
            auto frames = int(std::min<std::int64_t>(BLOCK, std::max<std::int64_t>(0, limit - master)));
            if (!frames)
            {
                bool repeat = false;
                {
                    std::lock_guard<std::mutex> lock(mutex);
                    if (requestedGeneration != generation)
                        continue;
                    repeat = looping;
                    snapshot.ready = true;
                    if (!repeat)
                        ended = true;
                }
                if (repeat)
                {
                    // Gapless group loop: append the next opening to the same
                    // PCM queue. Do not stop/reset/restart the audio endpoint.
                    master = 0;
                    for (std::size_t i = 0; i < inputs.size(); ++i)
                    {
                        auto &input = *inputs[i];
                        input.segments.clear();
                        input.ended = false;
                        input.end = 0;
                        auto duration = input.decoder.info().duration;
                        if (i > 0 && duration > 0 && voices[i].offset - double(TAPS) / rate >= duration)
                        {
                            input.ended = true;
                            input.end = std::int64_t(std::llround(duration * rate));
                            continue;
                        }
                        if (!input.decoder.seek(std::max(0.0, voices[i].offset - double(TAPS) / rate)))
                        {
                            fail(L"A Stack voice could not loop.");
                            return;
                        }
                    }
                }
                continue;
            }
            std::vector<std::int64_t> starts;
            std::vector<std::array<double, TAPS>> weights;
            std::vector<bool> exact;
            std::wstring error;
            double totalGain = 0;
            for (std::size_t i = 0; i < inputs.size(); ++i)
            {
                double delta = voices[i].offset * rate;
                auto whole = std::int64_t(std::floor(delta));
                double fraction = delta - double(whole);
                bool integer = fraction < 1e-8 || fraction > 1 - 1e-8;
                if (fraction > 1 - 1e-8)
                    ++whole;
                starts.push_back(master + whole);
                exact.push_back(integer);
                weights.push_back(kernel(fraction));
                totalGain += voices[i].gain;
                if (!inputs[i]->ensure(master + whole + frames + TAPS / 2, closing, error))
                {
                    if (!closing)
                        fail(error);
                    return;
                }
            }
            if (inputs[0]->ended)
            {
                limit = std::min(limit, inputs[0]->end);
                if (limit <= 0)
                {
                    fail(L"The primary contains no decoded audio samples.");
                    return;
                }
                if (primary.duration != double(limit) / rate)
                {
                    primary.duration = double(limit) / rate;
                    std::lock_guard<std::mutex> lock(mutex);
                    snapshot.primary.duration = primary.duration;
                }
                frames = int(std::min<std::int64_t>(frames, std::max<std::int64_t>(0, limit - master)));
            }
            if (!frames)
                continue;
            Block block{{}, double(master) / rate, generation};
            block.pcm.resize(std::size_t(frames) * channels);
            double headroom = 1 / std::max(1.0, totalGain);
            for (int frame = 0; frame < frames; ++frame)
                for (int channel = 0; channel < channels; ++channel)
                {
                    double sum = 0;
                    for (std::size_t i = 0; i < inputs.size(); ++i)
                    {
                        if (!voices[i].gain)
                            continue;
                        auto index = starts[i] + frame;
                        double value = 0;
                        if (index >= 0 && exact[i])
                            value = inputs[i]->at(index, channel);
                        else if (index >= 0)
                            for (int tap = 0; tap < TAPS; ++tap)
                                value +=
                                    inputs[i]->at(index + tap - (TAPS / 2 - 1), channel) * weights[i][tap];
                        sum += std::isfinite(value) ? value * voices[i].gain : 0;
                    }
                    block.pcm[std::size_t(frame) * channels + channel] = float(sum * headroom);
                }
            for (std::size_t i = 0; i < inputs.size(); ++i)
                inputs[i]->discard(starts[i] + frames - TAPS);
            master += frames;
            {
                std::lock_guard<std::mutex> lock(mutex);
                if (generation != requestedGeneration)
                    continue; // Late work cannot leak across a seek/configuration epoch.
                snapshot.ready = snapshot.ready || queue.size() >= 2;
                snapshot.generation = generation;
                snapshot.mixedFrames += frames;
                snapshot.queuedFrames += frames;
                queue.push_back(std::move(block));
            }
        }
    }
};
StackPcm::StackPcm(std::vector<PcmVoice> voices, int rate, int channels)
    : impl(std::make_unique<Impl>(std::move(voices), rate, channels))
{
    impl->worker = std::thread([this] {
        try
        {
            impl->run();
        }
        catch (...)
        {
            impl->fail(L"Stack PCM worker failed.");
        }
    });
}
StackPcm::~StackPcm() = default;
PcmRead StackPcm::read(std::vector<float> &pcm, double &timestamp)
{
    std::lock_guard<std::mutex> lock(impl->mutex);
    pcm.clear();
    if (!impl->snapshot.error.empty())
        return PcmRead::Error;
    if (impl->queue.empty())
        return impl->ended ? PcmRead::End : PcmRead::Waiting;
    auto block = std::move(impl->queue.front());
    impl->queue.pop_front();
    impl->snapshot.queuedFrames -= block.pcm.size() / impl->channels;
    timestamp = block.timestamp;
    pcm = std::move(block.pcm);
    impl->wake.notify_one();
    return PcmRead::Data;
}
bool StackPcm::seek(double seconds, const std::vector<PcmVoice> &voices)
{
    if (!std::isfinite(seconds) || seconds < 0 || seconds > 1e9 || (!voices.empty() && !validVoices(voices)))
        return false;
    std::lock_guard<std::mutex> lock(impl->mutex);
    if (!voices.empty())
    {
        if (voices.size() != impl->configuration.size())
            return false;
        for (std::size_t i = 0; i < voices.size(); ++i)
            if (voices[i].path != impl->configuration[i].path)
                return false;
        impl->configuration = voices;
    }
    impl->requestedSeek = seconds;
    ++impl->requestedGeneration;
    impl->queue.clear();
    impl->snapshot.queuedFrames = 0;
    impl->snapshot.ready = false;
    impl->ended = false;
    impl->wake.notify_one();
    return true;
}
bool StackPcm::voices(const std::vector<PcmVoice> &voices)
{
    if (!validVoices(voices))
        return false;
    std::lock_guard<std::mutex> lock(impl->mutex);
    if (voices.size() != impl->configuration.size())
        return false;
    for (std::size_t i = 0; i < voices.size(); ++i)
        if (voices[i].path != impl->configuration[i].path ||
            voices[i].offset != impl->configuration[i].offset)
            return false; // Offset changes require one explicit common-clock seek.
    impl->configuration = voices;
    impl->wake.notify_one();
    return true;
}
void StackPcm::loop(bool enabled)
{
    std::lock_guard<std::mutex> lock(impl->mutex);
    if (impl->looping == enabled)
        return;
    impl->looping = enabled;
    if (enabled)
        impl->ended = false;
    impl->wake.notify_one();
}
PcmStackState StackPcm::state() const
{
    std::lock_guard<std::mutex> lock(impl->mutex);
    return impl->snapshot;
}
} // namespace ga
