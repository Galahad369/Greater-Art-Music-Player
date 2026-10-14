#pragma once
#include "Media.h"
#include <memory>
namespace ga
{
class AudioFallback
{
    struct Impl;
    std::unique_ptr<Impl> impl;

  public:
    AudioFallback(const std::wstring &, bool audioOnly);
    ~AudioFallback();
    HRESULT pause(bool);
    HRESULT seek(double);
    HRESULT rate(double);
    HRESULT volume(double);
    MediaState state() const;
};
} // namespace ga
