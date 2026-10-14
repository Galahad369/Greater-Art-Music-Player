#pragma once
#include <functional>
#include <memory>
#include <string>
#include <vector>
namespace ga
{
struct CodecInfo
{
    int rate = 0, channels = 0, width = 0, height = 0;
    double duration = 0;
    std::wstring title, artist, album, codec, error;
};
class CodecReader
{
    struct Impl;
    std::unique_ptr<Impl> impl;

  public:
    CodecReader();
    ~CodecReader();
    bool open(const std::wstring &, std::function<bool()> cancel, int outputRate = 0, int outputChannels = 0);
    bool read(std::vector<float> &, double &timestamp);
    bool seek(double seconds);
    CodecInfo info() const;
};
bool codecsAvailable();
std::wstring codecLicense();
} // namespace ga
