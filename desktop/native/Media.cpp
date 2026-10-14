#include "Media.h"
#include "AudioFallback.h"
#include "CodecReader.h"
#include "Features.h"
#include <atomic>
#include <cmath>
#include <functional>
#include <future>
#include <memory>
#include <mfapi.h>
#include <mfmediaengine.h>
#include <mutex>
#include <propvarutil.h>
#include <thread>
#include <unordered_set>
#include <wrl/client.h>

using Microsoft::WRL::ComPtr;
namespace ga
{
namespace
{
constexpr UINT TASK = WM_APP + 73;
std::thread worker;
DWORD workerId = 0;
std::mutex enqueue;
std::atomic<bool> running{false};
struct Player
{
    ComPtr<IMFMediaEngine> engine;
    ComPtr<IMFMediaEngineEx> extended;
    HWND video = nullptr, host = nullptr;
    int width = 0, height = 0;
    DWORD sourceWidth = 0, sourceHeight = 0;
    double pendingSeek = NAN;
    std::wstring path;
    bool audioOnly = false, desiredPause = true, fallbackAttempted = false;
    double rate = 1, gain = 1, lastSeek = 0;
    std::unique_ptr<AudioFallback> fallback;
    ~Player()
    {
        if (engine)
            engine->Shutdown();
        if (video)
            DestroyWindow(video);
    }
};
std::unordered_set<Player *> players;
class Notify final : public IMFMediaEngineNotify
{
    std::atomic<ULONG> refs{1};

  public:
    HRESULT STDMETHODCALLTYPE QueryInterface(REFIID id, void **out) override
    {
        if (!out)
            return E_POINTER;
        *out = nullptr;
        if (id == __uuidof(IUnknown) || id == __uuidof(IMFMediaEngineNotify))
        {
            *out = this;
            AddRef();
            return S_OK;
        }
        return E_NOINTERFACE;
    }
    ULONG STDMETHODCALLTYPE AddRef() override
    {
        return ++refs;
    }
    ULONG STDMETHODCALLTYPE Release() override
    {
        auto n = --refs;
        if (!n)
            delete this;
        return n;
    }
    HRESULT STDMETHODCALLTYPE EventNotify(DWORD, DWORD_PTR, DWORD) override
    {
        return S_OK;
    }
};
template <class T> T invoke(std::function<T()> task, T fallback)
{
    if (GetCurrentThreadId() == workerId)
        return task();
    auto packaged = std::make_shared<std::packaged_task<T()>>(std::move(task));
    auto result = packaged->get_future();
    auto dispatch = new std::function<void()>([packaged] { (*packaged)(); });
    {
        std::lock_guard<std::mutex> lock(enqueue);
        if (!running || !PostThreadMessage(workerId, TASK, 0, reinterpret_cast<LPARAM>(dispatch)))
        {
            delete dispatch;
            return fallback;
        }
    }
    // A renderer owned by this MTA thread can synchronously message the UI while
    // reparenting. Pump sent messages only, not queued clicks/timers, during a UI wait.
    while (result.wait_for(std::chrono::milliseconds(0)) != std::future_status::ready)
    {
        if (MsgWaitForMultipleObjectsEx(0, nullptr, 10, QS_SENDMESSAGE, MWMO_INPUTAVAILABLE) == WAIT_OBJECT_0)
        {
            MSG message{};
            PeekMessage(&message, nullptr, 0, 0, PM_NOREMOVE);
        }
    }
    try
    {
        return result.get();
    }
    catch (...)
    {
        return fallback;
    }
}
Player *valid(void *handle)
{
    auto p = static_cast<Player *>(handle);
    return players.count(p) ? p : nullptr;
}
bool startAudioFallback(Player *p)
{
    if (p->fallback)
        return true;
    if (p->fallbackAttempted || p->path.empty() || !codecsAvailable())
        return false;
    p->fallbackAttempted = true;
    p->engine->Pause();
    try
    {
        p->fallback = std::make_unique<AudioFallback>(p->path, p->audioOnly);
    }
    catch (...)
    {
        return false;
    }
    p->fallback->volume(p->gain);
    p->fallback->rate(p->rate);
    if (p->lastSeek > 0)
        p->fallback->seek(p->lastSeek);
    p->fallback->pause(p->desiredPause);
    return true;
}
void fit(Player *p)
{
    if (!p || !p->host || !p->video || !IsWindow(p->host))
        return;
    if (p->fallback)
    {
        ShowWindow(p->video, SW_HIDE);
        return;
    }
    RECT r{};
    GetClientRect(p->host, &r);
    DWORD w = 0, h = 0;
    p->engine->GetNativeVideoSize(&w, &h);
    if (r.right <= 0 || r.bottom <= 0)
        return;
    if (p->width == r.right && p->height == r.bottom && p->sourceWidth == w && p->sourceHeight == h)
        return;
    p->width = r.right;
    p->height = r.bottom;
    p->sourceWidth = w;
    p->sourceHeight = h;
    RECT destination = r;
    if (w && h)
    {
        double scale = (std::min)(double(r.right) / w, double(r.bottom) / h);
        int width = int(w * scale + .5), height = int(h * scale + .5);
        destination = {(r.right - width) / 2, (r.bottom - height) / 2, (r.right + width) / 2,
                       (r.bottom + height) / 2};
    }
    // HWND-mode presentation uses an origin-relative destination. Centre the owned
    // renderer window itself; otherwise a fitted frame can stick to the left edge.
    SetWindowPos(p->video, nullptr, destination.left, destination.top, destination.right - destination.left,
                 destination.bottom - destination.top, SWP_NOZORDER | SWP_NOACTIVATE);
    RECT local{0, 0, destination.right - destination.left, destination.bottom - destination.top};
    MFARGB black{0, 0, 0, 255};
    p->extended->UpdateVideoStream(nullptr, &local, &black);
    ShowWindow(p->video, w && h ? SW_SHOWNA : SW_HIDE);
}
LRESULT CALLBACK videoProc(HWND h, UINT m, WPARAM w, LPARAM l)
{
    if (m == WM_NCHITTEST)
        return HTTRANSPARENT;
    return DefWindowProc(h, m, w, l);
}
} // namespace

bool mediaInitialize()
{
    if (running)
        return false;
    std::promise<bool> started;
    auto result = started.get_future();
    worker = std::thread([&] {
        HRESULT com = CoInitializeEx(nullptr, COINIT_MULTITHREADED);
        HRESULT mf = SUCCEEDED(com) ? MFStartup(MF_VERSION) : com;
        WNDCLASS cls{};
        cls.lpfnWndProc = videoProc;
        cls.hInstance = GetModuleHandle(nullptr);
        cls.lpszClassName = L"GreaterArtRenderer";
        cls.hbrBackground = static_cast<HBRUSH>(GetStockObject(BLACK_BRUSH));
        RegisterClass(&cls);
        MSG message{};
        PeekMessage(&message, nullptr, 0, 0, PM_NOREMOVE);
        workerId = GetCurrentThreadId();
        running = SUCCEEDED(mf);
        started.set_value(running);
        if (SUCCEEDED(mf))
        {
            SetTimer(nullptr, 1, 40, nullptr);
            while (GetMessage(&message, nullptr, 0, 0) > 0)
            {
                if (message.message == TASK)
                {
                    std::unique_ptr<std::function<void()>> task(
                        reinterpret_cast<std::function<void()> *>(message.lParam));
                    (*task)();
                }
                else if (message.message == WM_TIMER)
                {
                    for (auto p : players)
                    {
                        ComPtr<IMFMediaError> error;
                        p->engine->GetError(&error);
                        if (!p->fallbackAttempted && !p->path.empty() && error && error->GetErrorCode() &&
                            codecsAvailable())
                        {
                            startAudioFallback(p);
                        }
                        if (p->fallback)
                        {
                            fit(p);
                            continue;
                        }
                        if (std::isfinite(p->pendingSeek) &&
                            p->engine->GetReadyState() >= MF_MEDIA_ENGINE_READY_HAVE_METADATA &&
                            SUCCEEDED(p->engine->SetCurrentTime(p->pendingSeek)))
                            p->pendingSeek = NAN;
                        fit(p);
                    }
                }
                else
                {
                    TranslateMessage(&message);
                    DispatchMessage(&message);
                }
            }
            MFShutdown();
        }
        if (SUCCEEDED(com))
            CoUninitialize();
    });
    if (!result.get())
    {
        worker.join();
        workerId = 0;
        return false;
    }
    return true;
}
void *mediaCreate(bool audioOnly)
{
    return invoke<void *>(
        [=]() -> void * {
            auto p = std::make_unique<Player>();
            p->audioOnly = audioOnly;
            ComPtr<IMFMediaEngineClassFactory> factory;
            if (FAILED(CoCreateInstance(CLSID_MFMediaEngineClassFactory, nullptr, CLSCTX_INPROC_SERVER,
                                        IID_PPV_ARGS(&factory))))
                return nullptr;
            ComPtr<IMFAttributes> attributes;
            if (FAILED(MFCreateAttributes(&attributes, 3)))
                return nullptr;
            ComPtr<Notify> callback;
            callback.Attach(new Notify());
            attributes->SetUnknown(MF_MEDIA_ENGINE_CALLBACK, callback.Get());
            if (!audioOnly)
            {
                p->video = CreateWindowEx(WS_EX_NOACTIVATE | WS_EX_NOPARENTNOTIFY, L"GreaterArtRenderer", L"",
                                          WS_POPUP | WS_CLIPSIBLINGS, 0, 0, 1, 1, nullptr, nullptr,
                                          GetModuleHandle(nullptr), nullptr);
                if (!p->video)
                    return nullptr;
                attributes->SetUINT64(MF_MEDIA_ENGINE_PLAYBACK_HWND, reinterpret_cast<UINT_PTR>(p->video));
            }
            if (FAILED(factory->CreateInstance(audioOnly ? MF_MEDIA_ENGINE_AUDIOONLY : 0, attributes.Get(),
                                               &p->engine)) ||
                FAILED(p->engine.As(&p->extended)))
                return nullptr;
            p->engine->SetAutoPlay(FALSE);
            auto raw = p.release();
            players.insert(raw);
            return raw;
        },
        nullptr);
}
HRESULT mediaLoad(void *handle, const std::wstring &path)
{
    // Local filesystem source only; no URL resolver, runtime download or player fallback.
    if (!localMediaPath(path))
        return E_INVALIDARG;
    DWORD attributes = GetFileAttributes(path.c_str());
    if (attributes == INVALID_FILE_ATTRIBUTES || attributes & FILE_ATTRIBUTE_DIRECTORY)
        return HRESULT_FROM_WIN32(ERROR_FILE_NOT_FOUND);
    return invoke<HRESULT>(
        [=] {
            auto p = valid(handle);
            if (!p)
                return E_HANDLE;
            p->engine->Pause();
            p->fallback.reset();
            p->fallbackAttempted = false;
            p->path = path;
            p->desiredPause = true;
            p->lastSeek = 0;
            p->pendingSeek = NAN;
            p->width = 0;
            BSTR source = SysAllocString(path.c_str());
            if (!source)
                return E_OUTOFMEMORY;
            HRESULT result = p->engine->SetSource(source);
            SysFreeString(source);
            if (SUCCEEDED(result))
                result = p->engine->Load();
            if (FAILED(result) && startAudioFallback(p))
                result = S_OK;
            return result;
        },
        E_FAIL);
}
HRESULT mediaPlay(void *handle)
{
    return invoke<HRESULT>(
        [=] {
            auto p = valid(handle);
            if (!p)
                return E_HANDLE;
            p->desiredPause = false;
            if (p->fallback)
            {
                if (p->fallback->state().state == 6)
                    p->fallback->seek(0);
                return p->fallback->pause(false);
            }
            if (p->engine->IsEnded())
                p->engine->SetCurrentTime(0);
            auto result = p->engine->Play();
            if (FAILED(result) && startAudioFallback(p))
                result = S_OK;
            return result;
        },
        E_FAIL);
}
HRESULT mediaPause(void *handle, bool pause)
{
    return invoke<HRESULT>(
        [=] {
            auto p = valid(handle);
            if (!p)
                return E_HANDLE;
            p->desiredPause = pause;
            return p->fallback ? p->fallback->pause(pause) : pause ? p->engine->Pause() : p->engine->Play();
        },
        E_FAIL);
}
HRESULT mediaSeek(void *handle, double seconds)
{
    return invoke<HRESULT>(
        [=] {
            auto p = valid(handle);
            if (!p || !std::isfinite(seconds))
                return E_INVALIDARG;
            p->lastSeek = (std::max)(0.0, seconds);
            if (p->fallback)
                return p->fallback->seek(p->lastSeek);
            if (p->engine->GetReadyState() < MF_MEDIA_ENGINE_READY_HAVE_METADATA)
            {
                p->pendingSeek = (std::max)(0.0, seconds);
                return S_OK;
            }
            return p->engine->SetCurrentTime((std::max)(0.0, seconds));
        },
        E_FAIL);
}
HRESULT mediaRate(void *handle, double rate)
{
    return invoke<HRESULT>(
        [=] {
            auto p = valid(handle);
            if (!p || !std::isfinite(rate) || rate < .5 || rate > 4)
                return E_INVALIDARG;
            p->rate = rate;
            return p->fallback ? p->fallback->rate(rate) : p->engine->SetPlaybackRate(rate);
        },
        E_FAIL);
}
HRESULT mediaVolume(void *handle, double gain)
{
    return invoke<HRESULT>(
        [=] {
            auto p = valid(handle);
            if (!p || !std::isfinite(gain))
                return E_INVALIDARG;
            p->gain = std::clamp(gain, 0.0, 1.0);
            return p->fallback ? p->fallback->volume(p->gain) : p->engine->SetVolume(p->gain);
        },
        E_FAIL);
}
HRESULT mediaAttach(void *handle, HWND host)
{
    return invoke<HRESULT>(
        [=] {
            auto p = valid(handle);
            if (!p || !p->video || !IsWindow(host))
                return E_INVALIDARG;
            if (p->host == host && GetParent(p->video) == host)
            {
                fit(p);
                return S_OK;
            }
            p->host = host;
            p->width = 0;
            SetLastError(0);
            SetParent(p->video, host);
            if (GetLastError())
                return HRESULT_FROM_WIN32(GetLastError());
            SetWindowLongPtr(p->video, GWL_STYLE, WS_CHILD | WS_VISIBLE | WS_CLIPSIBLINGS);
            fit(p);
            return S_OK;
        },
        E_FAIL);
}
void mediaDetach(void *handle)
{
    invoke<int>(
        [=] {
            auto p = valid(handle);
            if (p && p->video)
            {
                ShowWindow(p->video, SW_HIDE);
                SetParent(p->video, nullptr);
                SetWindowLongPtr(p->video, GWL_STYLE, WS_POPUP);
                p->host = nullptr;
            }
            return 0;
        },
        0);
}
MediaState mediaState(void *handle)
{
    return invoke<MediaState>(
        [=] {
            auto p = valid(handle);
            MediaState s;
            if (!p)
                return s;
            ComPtr<IMFMediaError> error;
            p->engine->GetError(&error);
            // State reads and timer reconciliation use the same handoff. Never
            // expose native failure as terminal while an eligible fallback starts.
            if (!p->fallback && error && error->GetErrorCode())
                startAudioFallback(p);
            if (p->fallback)
            {
                auto state = p->fallback->state();
                if (state.state == 0)
                    state.state = 1;
                state.bundledAudio = true;
                return state;
            }
            s.time = p->engine->GetCurrentTime();
            s.duration = p->engine->GetDuration();
            if (!std::isfinite(s.time))
                s.time = 0;
            if (!std::isfinite(s.duration))
                s.duration = 0;
            s.state = error && error->GetErrorCode()                                         ? 7
                      : p->engine->IsEnded()                                                 ? 6
                      : p->engine->GetReadyState() < MF_MEDIA_ENGINE_READY_HAVE_CURRENT_DATA ? 1
                      : p->engine->IsPaused()                                                ? 4
                                                                                             : 3;
            p->engine->GetNativeVideoSize(&s.width, &s.height);
            PROPVARIANT frames;
            PropVariantInit(&frames);
            if (SUCCEEDED(p->extended->GetStatistics(MF_MEDIA_ENGINE_STATISTIC_FRAMES_RENDERED, &frames)))
                s.frames = frames.vt == VT_UI4   ? frames.ulVal
                           : frames.vt == VT_UI8 ? frames.uhVal.QuadPart
                                                 : 0;
            PropVariantClear(&frames);
            return s;
        },
        {});
}
void mediaRelease(void *handle)
{
    invoke<int>(
        [=] {
            auto p = valid(handle);
            if (p)
            {
                players.erase(p);
                delete p;
            }
            return 0;
        },
        0);
}
void mediaShutdown()
{
    if (!running)
        return;
    invoke<int>(
        [] {
            for (auto p : players)
                delete p;
            players.clear();
            return 0;
        },
        0);
    {
        std::lock_guard<std::mutex> lock(enqueue);
        running = false;
        PostThreadMessage(workerId, WM_QUIT, 0, 0);
    }
    worker.join();
    workerId = 0;
}
} // namespace ga
