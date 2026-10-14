#pragma once
#define WIN32_LEAN_AND_MEAN
#include <cstdint>
#include <string>
#include <windows.h>
#include "VideoAmbient.h"

namespace ga
{
struct MediaState
{
    double time = 0, duration = 0;
    int state = 0;
    DWORD width = 0, height = 0;
    std::uint64_t frames = 0;
    bool bundledAudio = false;
    bool sharedClock = false;
    int pcmRate = 0, pcmChannels = 0;
    std::uint64_t pcmUnderruns = 0;
    VideoPalette palette;
    unsigned videoError = 0;
    HRESULT videoStatus = S_OK;
    bool ambientUnavailable = false;
};
bool mediaInitialize();
void mediaShutdown();
void *mediaCreate(bool audioOnly);
void mediaRelease(void *player);
HRESULT mediaLoad(void *player, const std::wstring &path);
HRESULT mediaPlay(void *player);
HRESULT mediaPause(void *player, bool pause);
HRESULT mediaSeek(void *player, double seconds);
HRESULT mediaRate(void *player, double rate);
HRESULT mediaVolume(void *player, double volume);
HRESULT mediaAttach(void *player, HWND host);
void mediaDetach(void *player);
MediaState mediaState(void *player);
void mediaAmbientEnable(void *player, bool enabled);
// Owned-fixture verification only; never called by normal UI or file metadata.
void mediaAmbientFaultForTest(void *player);
} // namespace ga
