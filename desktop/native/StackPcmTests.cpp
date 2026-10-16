#include "StackPcm.h"
#include <algorithm>
#include <chrono>
#include <cmath>
#include <filesystem>
#include <fstream>
#include <thread>

namespace ga
{
namespace
{
void fixture(const std::filesystem::path &path, const std::vector<float> &pcm, int rate = 48000)
{
    std::ofstream out(path, std::ios::binary);
    std::uint32_t length = std::uint32_t(pcm.size() * sizeof(float)), size = length + 36, fmtLength = 16,
                  bytes = rate * 4;
    std::uint16_t format = 3, channels = 1, align = 4, bits = 32;
    auto write = [&](const auto &value) { out.write(reinterpret_cast<const char *>(&value), sizeof(value)); };
    out.write("RIFF", 4);
    write(size);
    out.write("WAVEfmt ", 8);
    write(fmtLength);
    write(format);
    write(channels);
    write(rate);
    write(bytes);
    write(align);
    write(bits);
    out.write("data", 4);
    write(length);
    out.write(reinterpret_cast<const char *>(pcm.data()), length);
}
bool take(StackPcm &stack, std::vector<float> &pcm, double &time)
{
    auto until = std::chrono::steady_clock::now() + std::chrono::seconds(5);
    while (std::chrono::steady_clock::now() < until)
    {
        auto read = stack.read(pcm, time);
        if (read == PcmRead::Data)
            return true;
        if (read != PcmRead::Waiting)
            return false;
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
    }
    return false;
}
} // namespace
bool stackPcmTests(const std::wstring &directory)
{
    auto root = std::filesystem::path(directory) / L"pcm-fixtures";
    std::filesystem::create_directories(root);
    std::ofstream report(std::filesystem::path(directory) / L"stack-pcm-tests.txt");
    bool passed = true;
    auto expect = [&](bool value, const char *name) {
        report << (value ? "PASS " : "FAIL ") << name << '\n';
        passed = passed && value;
    };
    constexpr int rate = 48000, count = 24000;
    std::vector<float> reference(count);
    for (int i = 0; i < count; ++i)
        reference[i] = float(.3 * std::sin(i * .04) + .08 * std::cos(i * .133));
    fixture(root / L"primary.wav", reference);
    std::vector<PcmVoice> voices;
    for (int voice = 0; voice < 6; ++voice)
    {
        int delay = voice * 37;
        std::vector<float> data(count + delay + 500, 0);
        std::copy(reference.begin(), reference.end(), data.begin() + delay);
        auto path = root / (L"voice-" + std::to_wstring(voice) + L".wav");
        fixture(path, data);
        voices.push_back({path.wstring(), 1, double(delay) / rate});
    }
    // Primary owns duration; companions intentionally have different lengths.
    voices[0].path = (root / L"primary.wav").wstring();
    {
        StackPcm stack(voices, rate, 1);
        std::vector<float> block, actual;
        double time = 0, error = 0;
        while (take(stack, block, time))
        {
            expect(std::abs(time * rate - actual.size()) < .0001, "every block has one integer master clock");
            actual.insert(actual.end(), block.begin(), block.end());
        }
        if (actual.size() == reference.size())
            for (std::size_t i = 0; i < actual.size(); ++i)
                error = std::max(error, std::abs(double(actual[i] - reference[i])));
        else
            error = 1;
        expect(error < 1e-6, "six integer-offset voices mix without relative sample drift");
        expect(actual.size() == reference.size(), "primary ends group; longer companions do not extend it");
        expect(stack.state().queuedFrames <= 6 * 1024, "prebuffer is bounded to six blocks");
        expect(stack.seek(.2), "common seek accepted after EOF");
        bool ready = take(stack, block, time);
        expect(ready && std::abs(time - .2) < 1e-9 && std::abs(block[0] - reference[9600]) < 1e-6,
               "seek discards old-generation samples");
    }
    {
        std::vector<float> delayed(count);
        for (int i = 0; i < count; ++i)
            delayed[i] = float(.3 * std::sin((i - .5) * .04) + .08 * std::cos((i - .5) * .133));
        fixture(root / L"fractional.wav", delayed);
        StackPcm stack(
            {{(root / L"primary.wav").wstring(), 0, 0}, {(root / L"fractional.wav").wstring(), 1, .5 / rate}},
            rate, 1);
        std::vector<float> block;
        double time = 0, error = 1;
        if (take(stack, block, time))
        {
            error = 0;
            for (int i = 32; i < int(block.size()) - 32; ++i)
                error = std::max(error, std::abs(double(block[i] - reference[i])));
        }
        expect(error < .0001, "half-sample offset uses bandlimited interpolation, not rounded seeks");
    }
    {
        StackPcm stack({{(root / L"primary.wav").wstring(), 0, 0},
                        {(root / L"primary.wav").wstring(), 1, -100.0 / rate}},
                       rate, 1);
        std::vector<float> block;
        double time = 0;
        bool ready = take(stack, block, time);
        expect(ready &&
                   std::all_of(block.begin(), block.begin() + 100, [](float value) { return value == 0; }),
               "negative-offset voice waits in silence, never repeats its opening");
        expect(ready && std::abs(block[100] - reference[0]) < 1e-6,
               "delayed voice enters at the exact shared frame");
    }
    {
        StackPcm stack({{L"https://invalid.example/audio.wav", 1, 0}}, rate, 1);
        std::vector<float> block;
        double time = 0;
        expect(!take(stack, block, time) && !stack.state().error.empty(), "Stack rejects network sources");
    }
    {
        StackPcm stack(voices, rate, 2);
        std::vector<float> block;
        double time = 0;
        bool ready = take(stack, block, time);
        expect(ready && block.size() % 2 == 0, "voices are converted to the actual output channel layout");
        auto bad = voices;
        bad[0].offset = .1;
        expect(!stack.voices(bad) && !stack.seek(0, bad), "primary offset and invalid commands rejected");
        bad = voices;
        bad[1].gain = .1;
        expect(stack.voices(bad), "gain updates do not restart the common clock");
        expect(stack.seek(0), "whole group can restart from primary opening");
    }
    {
        auto eight = voices;
        eight.push_back(voices[1]);
        eight.push_back(voices[2]);
        StackPcm stack(eight, rate, 1);
        std::vector<float> block;
        double time = 0, error = 1;
        if (take(stack, block, time))
        {
            error = 0;
            for (std::size_t i = 0; i < block.size(); ++i)
                error = std::max(error, std::abs(double(block[i] - reference[i])));
        }
        expect(error < 1e-6, "eight voices share the same sample clock and coherent headroom");
    }
    {
        StackPcm stack(
            {{(root / L"primary.wav").wstring(), 1, 0}, {(root / L"primary.wav").wstring(), 1, 30}}, rate, 1);
        std::vector<float> block;
        double time = 0;
        bool ready = take(stack, block, time);
        expect(ready && std::abs(block[0] - reference[0] / 2) < 1e-6,
               "offset beyond a companion EOF contributes silence rather than failing the whole group");
    }
    {
        StackPcm stack(voices, rate, 1);
        stack.loop(true);
        std::vector<float> block;
        double time = 0, error = 0;
        std::size_t received = 0;
        bool ready = true;
        while (received < 3 * reference.size() && ready)
        {
            ready = take(stack, block, time);
            for (std::size_t i = 0; ready && i < block.size(); ++i)
                error = std::max(error,
                                 std::abs(double(block[i] - reference[(received + i) % reference.size()])));
            received += ready ? block.size() : 0;
        }
        expect(ready && error < 1e-6 && received >= 3 * reference.size(),
               "three whole-group loops are contiguous sample blocks, not endpoint restarts");
    }
    return passed;
}
} // namespace ga
