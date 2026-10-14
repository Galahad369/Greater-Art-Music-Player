#pragma once
#include <windows.h>
#include <mftransform.h>
#include <array>
#include <cstdint>
#include <atomic>
#include <memory>
#include <mutex>
#include <string>

namespace ga
{
struct VideoPalette
{
    std::array<COLORREF, 2> colors{};
    std::uint64_t generation = 0, samples = 0;
    std::uint64_t framesSeen = 0, gpuSamples = 0;
    float brightness = 0, saturation = 0;
};
class VideoAmbientTap
{
    mutable std::mutex mutex;
    VideoPalette palette;

  public:
    std::atomic<bool> enabled{false};
    std::atomic<std::uint64_t> framesSeen{0};
    std::atomic<bool> failOnceForTest{false};
    void reset();
    VideoPalette snapshot() const;
    void publish(const std::array<COLORREF, 36> &pixels, std::uint64_t generation, bool gpu = false);
};
HRESULT createVideoAmbientEffect(std::shared_ptr<VideoAmbientTap>, IMFTransform **);
inline bool ambientSamplingAllowed(bool visible, bool fullscreen, bool highContrast, bool rejected, int mode)
{
    return visible && !fullscreen && !highContrast && !rejected && (mode == 1 || mode == 2);
}
bool videoAmbientTests(const std::wstring &directory);
} // namespace ga
