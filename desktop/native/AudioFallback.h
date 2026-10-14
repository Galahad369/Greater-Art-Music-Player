#pragma once
#include "Media.h"
#include "StackPcm.h"
#include <memory>
namespace ga
{
class AudioFallback
{
    struct Impl;
    std::unique_ptr<Impl> impl;

  public:
    AudioFallback(const std::wstring &, bool audioOnly);
    explicit AudioFallback(std::vector<PcmVoice>);
    ~AudioFallback();
    HRESULT pause(bool);
    HRESULT seek(double);
    HRESULT rate(double);
    HRESULT volume(double);
    HRESULT voices(const std::vector<PcmVoice> &);
    void loop(bool);
    MediaState state() const;
};
} // namespace ga
