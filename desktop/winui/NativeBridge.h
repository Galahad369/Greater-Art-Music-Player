#pragma once
#include <cstdint>
#include <windows.h>

// Blittable boundary shared by the WinUI process and the existing native engine.
// Ownership stays native; a presentation change never creates another player.
struct GA_State
{
    double position;
    double duration;
    std::int32_t state;
    std::int32_t width;
    std::int32_t height;
    std::uint64_t frames;
    std::uint64_t identity;
    std::int32_t bundledAudio;
    std::uint32_t videoError;
    std::uint32_t palette0;
    std::uint32_t palette1;
    std::uint64_t paletteSamples;
};

#define GA_EXPORT extern "C" __declspec(dllexport)
GA_EXPORT int GA_Initialize() noexcept;
GA_EXPORT void GA_Shutdown() noexcept;
GA_EXPORT void *GA_Create() noexcept;
GA_EXPORT void GA_Release(void *player) noexcept;
GA_EXPORT HRESULT GA_Load(void *player, const wchar_t *path) noexcept;
GA_EXPORT HRESULT GA_Play(void *player) noexcept;
GA_EXPORT HRESULT GA_Pause(void *player, int paused) noexcept;
GA_EXPORT HRESULT GA_Seek(void *player, double seconds) noexcept;
GA_EXPORT HRESULT GA_Rate(void *player, double rate) noexcept;
GA_EXPORT HRESULT GA_Volume(void *player, double volume) noexcept;
GA_EXPORT HRESULT GA_AmbientEnable(void *player, int enabled) noexcept;
GA_EXPORT HWND GA_CreateHost(HWND parent) noexcept;
GA_EXPORT HRESULT GA_SetHostBounds(HWND host, int x, int y, int width, int height, int visible) noexcept;
GA_EXPORT void GA_DestroyHost(HWND host) noexcept;
GA_EXPORT HRESULT GA_Attach(void *player, HWND host) noexcept;
GA_EXPORT HRESULT GA_GetState(void *player, GA_State *state) noexcept;
GA_EXPORT HRESULT GA_CaptureHost(HWND host, const wchar_t *path) noexcept;
