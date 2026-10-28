#define NOMINMAX
#include "NativeBridge.h"
#include "../native/Media.h"
#include <cstddef>
#include <chrono>
#include <d3d11.h>
#include <filesystem>
#include <fstream>
#include <future>
#include <mutex>
#include <thread>
#include <unordered_set>
#include <vector>
#include <windows.graphics.capture.interop.h>
#include <windows.graphics.directx.direct3d11.interop.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Graphics.Capture.h>
#include <winrt/Windows.Graphics.DirectX.Direct3D11.h>

namespace
{
constexpr wchar_t hostClass[] = L"GreaterArt.WinUI.VideoHost";
std::mutex hostsMutex;
std::unordered_set<HWND> hosts;

bool ownedHost(HWND host)
{
    std::lock_guard lock(hostsMutex);
    return hosts.contains(host) && IsWindow(host);
}
bool processWindow(HWND window)
{
    DWORD process = 0;
    return IsWindow(window) && GetWindowThreadProcessId(window, &process) && process == GetCurrentProcessId();
}
LRESULT CALLBACK hostProc(HWND window, UINT message, WPARAM wparam, LPARAM lparam)
{
    // The host contains only video. WinUI keeps pointer/keyboard interaction and
    // primary controls outside its bounds; there is no independent player UI here.
    if (message == WM_NCHITTEST)
        return HTTRANSPARENT;
    return DefWindowProc(window, message, wparam, lparam);
}
template <class Function> HRESULT command(Function action) noexcept
{
    try { return action(); }
    catch (const std::bad_alloc &) { return E_OUTOFMEMORY; }
    catch (...) { return E_FAIL; }
}

HRESULT writeBitmap(const std::wstring &path, int width, int height, const void *pixels, std::size_t stride)
{
    if (width <= 0 || height <= 0 || !pixels || stride < static_cast<std::size_t>(width) * 4)
        return E_INVALIDARG;
    BITMAPFILEHEADER file{};
    BITMAPINFOHEADER info{};
    file.bfType = 0x4d42;
    file.bfOffBits = sizeof(file) + sizeof(info);
    const auto bytes = static_cast<std::uint64_t>(width) * height * 4;
    if (bytes > MAXDWORD - file.bfOffBits) return E_INVALIDARG;
    file.bfSize = file.bfOffBits + static_cast<DWORD>(bytes);
    info.biSize = sizeof(info);
    info.biWidth = width;
    info.biHeight = -height;
    info.biPlanes = 1;
    info.biBitCount = 32;
    info.biCompression = BI_RGB;
    std::ofstream output(std::filesystem::path(path), std::ios::binary);
    output.write(reinterpret_cast<const char *>(&file), sizeof(file));
    output.write(reinterpret_cast<const char *>(&info), sizeof(info));
    for (int y = 0; y < height && output; ++y)
        output.write(static_cast<const char *>(pixels) + y * stride, static_cast<std::streamsize>(width) * 4);
    return output ? S_OK : E_FAIL;
}

HRESULT captureCompositionWindow(HWND window, const std::wstring &path)
{
    using namespace winrt::Windows::Graphics;
    using namespace winrt::Windows::Graphics::Capture;
    using namespace winrt::Windows::Graphics::DirectX;
    using namespace winrt::Windows::Graphics::DirectX::Direct3D11;
    if (!processWindow(window) || !IsWindowVisible(window) || IsIconic(window)) return E_INVALIDARG;
    if (!GraphicsCaptureSession::IsSupported()) return E_NOTIMPL;
    GraphicsCaptureItem item{nullptr};
    auto interop = winrt::get_activation_factory<GraphicsCaptureItem, IGraphicsCaptureItemInterop>();
    winrt::check_hresult(interop->CreateForWindow(window, winrt::guid_of<GraphicsCaptureItem>(), winrt::put_abi(item)));
    auto size = item.Size();
    if (size.Width <= 0 || size.Height <= 0) return E_INVALIDARG;

    winrt::com_ptr<ID3D11Device> device;
    winrt::com_ptr<ID3D11DeviceContext> context;
    winrt::check_hresult(D3D11CreateDevice(nullptr, D3D_DRIVER_TYPE_HARDWARE, nullptr, D3D11_CREATE_DEVICE_BGRA_SUPPORT,
                                         nullptr, 0, D3D11_SDK_VERSION, device.put(), nullptr, context.put()));
    auto dxgi = device.as<IDXGIDevice>();
    winrt::com_ptr<IInspectable> inspectable;
    winrt::check_hresult(CreateDirect3D11DeviceFromDXGIDevice(dxgi.get(), inspectable.put()));
    auto projected = inspectable.as<IDirect3DDevice>();
    auto pool = Direct3D11CaptureFramePool::CreateFreeThreaded(projected,
                    DirectXPixelFormat::B8G8R8A8UIntNormalized, 2, size);
    auto session = pool.CreateCaptureSession(item);
    struct CloseCapture
    {
        GraphicsCaptureSession session;
        Direct3D11CaptureFramePool pool;
        ~CloseCapture()
        {
            try { session.Close(); pool.Close(); }
            catch (...) { }
        }
    } close{session, pool};
    // This endpoint captures only an explicitly checked owned HWND. It never
    // invokes the picker, requests a monitor, or captures the cursor/other apps.
    session.IsCursorCaptureEnabled(false);
    session.StartCapture();
    const auto deadline = std::chrono::steady_clock::now() + std::chrono::seconds(2);
    Direct3D11CaptureFrame frame{nullptr};
    while (std::chrono::steady_clock::now() < deadline)
    {
        frame = pool.TryGetNextFrame();
        if (frame) break;
        std::this_thread::sleep_for(std::chrono::milliseconds(5));
    }
    if (!frame) return HRESULT_FROM_WIN32(ERROR_TIMEOUT);
    auto content = frame.ContentSize();
    auto surface = frame.Surface().as<::Windows::Graphics::DirectX::Direct3D11::IDirect3DDxgiInterfaceAccess>();
    winrt::com_ptr<ID3D11Texture2D> texture;
    winrt::check_hresult(surface->GetInterface(__uuidof(ID3D11Texture2D), texture.put_void()));
    D3D11_TEXTURE2D_DESC description{};
    texture->GetDesc(&description);
    if (content.Width <= 0 || content.Height <= 0 || content.Width > static_cast<int>(description.Width) ||
        content.Height > static_cast<int>(description.Height)) return E_FAIL;
    description.Usage = D3D11_USAGE_STAGING;
    description.BindFlags = 0;
    description.CPUAccessFlags = D3D11_CPU_ACCESS_READ;
    description.MiscFlags = 0;
    winrt::com_ptr<ID3D11Texture2D> staging;
    winrt::check_hresult(device->CreateTexture2D(&description, nullptr, staging.put()));
    context->CopyResource(staging.get(), texture.get());
    context->Flush();
    D3D11_MAPPED_SUBRESOURCE mapped{};
    HRESULT copied = DXGI_ERROR_WAS_STILL_DRAWING;
    while (std::chrono::steady_clock::now() < deadline)
    {
        copied = context->Map(staging.get(), 0, D3D11_MAP_READ, D3D11_MAP_FLAG_DO_NOT_WAIT, &mapped);
        if (copied != DXGI_ERROR_WAS_STILL_DRAWING) break;
        std::this_thread::sleep_for(std::chrono::milliseconds(5));
    }
    if (copied == DXGI_ERROR_WAS_STILL_DRAWING) return HRESULT_FROM_WIN32(ERROR_TIMEOUT);
    winrt::check_hresult(copied);
    struct Unmap
    {
        ID3D11DeviceContext *context;
        ID3D11Texture2D *texture;
        ~Unmap() { context->Unmap(texture, 0); }
    } unmap{context.get(), staging.get()};
    return writeBitmap(path, content.Width, content.Height, mapped.pData, mapped.RowPitch);
}

HRESULT captureComposed(HWND window, const std::wstring &path)
{
    auto result = std::async(std::launch::async, [window, path]() -> HRESULT {
        try
        {
            winrt::init_apartment(winrt::apartment_type::multi_threaded);
            struct Apartment { ~Apartment() { winrt::uninit_apartment(); } } apartment;
            return captureCompositionWindow(window, path);
        }
        catch (const winrt::hresult_error &error) { return error.code(); }
        catch (const std::bad_alloc &) { return E_OUTOFMEMORY; }
        catch (...) { return E_FAIL; }
    });
    // As in the existing media bridge, service sent HWND messages but never
    // dispatch queued clicks/timers recursively while the diagnostic is waiting.
    while (result.wait_for(std::chrono::milliseconds(0)) != std::future_status::ready)
        if (MsgWaitForMultipleObjectsEx(0, nullptr, 10, QS_SENDMESSAGE, MWMO_INPUTAVAILABLE) == WAIT_OBJECT_0)
        {
            MSG message{};
            PeekMessage(&message, nullptr, 0, 0, PM_NOREMOVE);
        }
    return result.get();
}
}

static_assert(offsetof(GA_State, frames) == 32);
static_assert(offsetof(GA_State, paletteSamples) == 64);
static_assert(sizeof(GA_State) == 72);

int GA_Initialize() noexcept
{
    try { return ga::mediaInitialize() ? 1 : 0; }
    catch (...) { return 0; }
}
void GA_Shutdown() noexcept
{
    try { ga::mediaShutdown(); }
    catch (...) { }
}
void *GA_Create() noexcept
{
    try { return ga::mediaCreate(false); }
    catch (...) { return nullptr; }
}
void GA_Release(void *player) noexcept
{
    try { if (player) ga::mediaRelease(player); }
    catch (...) { }
}
HRESULT GA_Load(void *player, const wchar_t *path) noexcept
{
    if (!player || !path || !*path) return E_INVALIDARG;
    return command([&] { return ga::mediaLoad(player, path); });
}
HRESULT GA_Play(void *player) noexcept
{
    return command([&] { return ga::mediaPlay(player); });
}
HRESULT GA_Pause(void *player, int paused) noexcept
{
    return command([&] { return ga::mediaPause(player, paused != 0); });
}
HRESULT GA_Seek(void *player, double seconds) noexcept
{
    return command([&] { return ga::mediaSeek(player, seconds); });
}
HRESULT GA_Rate(void *player, double rate) noexcept
{
    return command([&] { return ga::mediaRate(player, rate); });
}
HRESULT GA_Volume(void *player, double volume) noexcept
{
    return command([&] { return ga::mediaVolume(player, volume); });
}
HRESULT GA_AmbientEnable(void *player, int enabled) noexcept
{
    if (!player) return E_INVALIDARG;
    return command([&] { ga::mediaAmbientEnable(player, enabled != 0); return S_OK; });
}
HWND GA_CreateHost(HWND parent) noexcept
{
    if (!IsWindow(parent)) return nullptr;
    try
    {
        WNDCLASSW cls{};
        cls.hInstance = GetModuleHandle(nullptr);
        cls.lpfnWndProc = hostProc;
        cls.lpszClassName = hostClass;
        cls.hbrBackground = static_cast<HBRUSH>(GetStockObject(BLACK_BRUSH));
        if (!RegisterClassW(&cls) && GetLastError() != ERROR_CLASS_ALREADY_EXISTS)
            return nullptr;
        HWND host = CreateWindowExW(WS_EX_NOACTIVATE, hostClass, L"", WS_CHILD | WS_CLIPCHILDREN | WS_CLIPSIBLINGS,
                                    0, 0, 1, 1, parent, nullptr, cls.hInstance, nullptr);
        if (host)
        {
            std::lock_guard lock(hostsMutex);
            hosts.insert(host);
        }
        return host;
    }
    catch (...) { return nullptr; }
}
HRESULT GA_SetHostBounds(HWND host, int x, int y, int width, int height, int visible) noexcept
{
    return command([&] {
        if (!ownedHost(host) || width < 0 || height < 0) return E_INVALIDARG;
        if (!SetWindowPos(host, HWND_TOP, x, y, width, height,
                          SWP_NOACTIVATE | (visible && width && height ? SWP_SHOWWINDOW : SWP_HIDEWINDOW)))
            return HRESULT_FROM_WIN32(GetLastError());
        return S_OK;
    });
}
void GA_DestroyHost(HWND host) noexcept
{
    try
    {
        if (!ownedHost(host)) return;
        // Call after transferring or releasing the player; destroying a parent
        // HWND while its native video child is attached would destroy that child.
        DestroyWindow(host);
        std::lock_guard lock(hostsMutex);
        hosts.erase(host);
    }
    catch (...) { }
}
HRESULT GA_Attach(void *player, HWND host) noexcept
{
    return command([&] {
        if (!ownedHost(host)) return E_INVALIDARG;
        return ga::mediaAttach(player, host);
    });
}
HRESULT GA_GetState(void *player, GA_State *state) noexcept
{
    if (!player || !state) return E_INVALIDARG;
    return command([&] {
        auto source = ga::mediaState(player);
        *state = {source.time, source.duration, source.state, static_cast<int>(source.width),
                  static_cast<int>(source.height), source.frames, reinterpret_cast<std::uint64_t>(player),
                  source.bundledAudio ? 1 : 0, source.videoError, source.palette.colors[0],
                  source.palette.colors[1], source.palette.samples};
        return S_OK;
    });
}
HRESULT GA_CaptureHost(HWND host, const wchar_t *path) noexcept
{
    if (!processWindow(host) || !path || !*path) return E_INVALIDARG;
    return command([&] {
        // GDI cannot prove composition-backed WinUI pixels. Use the OS's
        // window-specific compositor capture for owned top-level windows.
        if (!ownedHost(host)) return captureComposed(host, path);
        RECT bounds{};
        if (!GetClientRect(host, &bounds) || bounds.right <= 0 || bounds.bottom <= 0) return E_INVALIDARG;
        HDC source = GetDC(host);
        if (!source) return E_FAIL;
        HDC memory = CreateCompatibleDC(source);
        BITMAPINFO info{};
        info.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
        info.bmiHeader.biWidth = bounds.right;
        info.bmiHeader.biHeight = -bounds.bottom;
        info.bmiHeader.biPlanes = 1;
        info.bmiHeader.biBitCount = 32;
        info.bmiHeader.biCompression = BI_RGB;
        void *pixels = nullptr;
        HBITMAP bitmap = memory ? CreateDIBSection(source, &info, DIB_RGB_COLORS, &pixels, nullptr, 0) : nullptr;
        if (!bitmap)
        {
            if (memory) DeleteDC(memory);
            ReleaseDC(host, source);
            return E_OUTOFMEMORY;
        }
        auto old = SelectObject(memory, bitmap);
        struct CaptureCleanup
        {
            HWND window;
            HDC source, memory;
            HBITMAP bitmap;
            HGDIOBJ old;
            ~CaptureCleanup()
            {
                SelectObject(memory, old);
                DeleteObject(bitmap);
                DeleteDC(memory);
                ReleaseDC(window, source);
            }
        } cleanup{host, source, memory, bitmap, old};
        // Capture only this process's window, never the desktop or another app.
        // GPU-composited surfaces may still be unavailable to these GDI APIs;
        // an image must be inspected rather than treating S_OK as frame proof.
        BOOL copied = PrintWindow(host, memory, PW_CLIENTONLY | 0x00000002 /* PW_RENDERFULLCONTENT */);
        if (!copied)
            copied = BitBlt(memory, 0, 0, bounds.right, bounds.bottom, source, 0, 0, SRCCOPY | CAPTUREBLT);
        GdiFlush();
        return copied ? writeBitmap(path, bounds.right, bounds.bottom, pixels,
                                    static_cast<std::size_t>(bounds.right) * 4) : E_FAIL;
    });
}
