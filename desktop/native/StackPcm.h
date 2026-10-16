#pragma once
#include "CodecReader.h"
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

namespace ga
{
struct PcmVoice
{
    std::wstring path;
    double gain = 1, offset = 0;
};
enum class PcmRead
{
    Waiting,
    Data,
    End,
    Error
};
struct PcmStackState
{
    CodecInfo primary;
    bool ready = false;
    std::uint64_t generation = 0, mixedFrames = 0;
    std::size_t queuedFrames = 0;
    std::wstring error;
};
// Original bounded, offline PCM producer. Every voice is evaluated against the
// same integer master frame, before the single output/tempo path consumes it.
class StackPcm
{
    struct Impl;
    std::unique_ptr<Impl> impl;

  public:
    StackPcm(std::vector<PcmVoice>, int rate, int channels);
    ~StackPcm();
    PcmRead read(std::vector<float> &, double &timestamp);
    bool seek(double, const std::vector<PcmVoice> &voices = {});
    bool voices(const std::vector<PcmVoice> &);
    void loop(bool);
    PcmStackState state() const;
};
bool stackPcmTests(const std::wstring &directory);
bool stackOutputTests(const std::wstring &directory);
} // namespace ga
