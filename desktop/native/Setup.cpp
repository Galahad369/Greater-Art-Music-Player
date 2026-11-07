#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <windows.h>
#include <atomic>
#include <bcrypt.h>
#include <dwmapi.h>
#include <filesystem>
#include <iomanip>
#include <msi.h>
#include <shellapi.h>
#include <shlobj.h>
#include <sstream>
#include <string>
#include <thread>
#include <vector>
#include <windowsx.h>

namespace
{
HWND mainWindow = nullptr, shortcut = nullptr, launch = nullptr, installButton = nullptr;
HFONT titleFont = nullptr, bodyFont = nullptr;
HBRUSH background = nullptr;
std::thread installer;
std::atomic<bool> busy{false};
UINT result = ERROR_INSTALL_FAILURE;
int dpi = 96;
std::filesystem::path payload;
bool createShortcut = true, launchApp = true;
int px(int n)
{
    return MulDiv(n, dpi, 96);
}
std::filesystem::path installFolder()
{
    PWSTR local = nullptr;
    SHGetKnownFolderPath(FOLDERID_LocalAppData, 0, nullptr, &local);
    auto folder = std::filesystem::path(local ? local : L".") / L"Programs/GreaterArtDesktop";
    if (local)
        CoTaskMemFree(local);
    return folder;
}
bool localExtractionWorkingDirectory()
{
    std::error_code error;
    const auto directory = std::filesystem::current_path(error);
    if (error || !directory.is_absolute())
        return false;
    const auto volume = directory.root_name().native();
    if (volume.size() != 2 || volume[1] != L':')
        return false;

    auto prefix = directory.root_path();
    const UINT drive = GetDriveTypeW(prefix.c_str());
    if (drive != DRIVE_FIXED && drive != DRIVE_REMOVABLE && drive != DRIVE_RAMDISK)
        return false;
    for (const auto &component : directory.relative_path())
    {
        prefix /= component;
        const DWORD attributes = GetFileAttributesW(prefix.c_str());
        if (attributes == INVALID_FILE_ATTRIBUTES || !(attributes & FILE_ATTRIBUTE_DIRECTORY) ||
            (attributes & FILE_ATTRIBUTE_REPARSE_POINT))
            return false;
    }
    return true;
}
bool extract(const std::filesystem::path &directory = {})
{
    auto module = GetModuleHandle(nullptr);
    auto resource = FindResource(module, MAKEINTRESOURCE(11), RT_RCDATA),
         hashResource = FindResource(module, MAKEINTRESOURCE(12), RT_RCDATA);
    if (!resource || !hashResource)
        return false;
    auto size = SizeofResource(module, resource);
    auto data = static_cast<const unsigned char *>(LockResource(LoadResource(module, resource)));
    auto expected = static_cast<const char *>(LockResource(LoadResource(module, hashResource)));
    auto expectedSize = SizeofResource(module, hashResource);
    if (!data || size < 512 || !expected || expectedSize < 64)
        return false;
    BCRYPT_ALG_HANDLE algorithm = nullptr;
    BCRYPT_HASH_HANDLE hash = nullptr;
    DWORD objectSize = 0, written = 0;
    std::vector<unsigned char> object;
    unsigned char digest[32]{};
    bool okay = BCryptOpenAlgorithmProvider(&algorithm, BCRYPT_SHA256_ALGORITHM, nullptr, 0) == 0;
    if (okay)
        okay = BCryptGetProperty(algorithm, BCRYPT_OBJECT_LENGTH, reinterpret_cast<PUCHAR>(&objectSize),
                                 sizeof(objectSize), &written, 0) == 0;
    if (okay)
    {
        object.resize(objectSize);
        okay = BCryptCreateHash(algorithm, &hash, object.data(), objectSize, nullptr, 0, 0) == 0;
    }
    if (okay)
        okay = BCryptHashData(hash, const_cast<PUCHAR>(data), size, 0) == 0 &&
               BCryptFinishHash(hash, digest, sizeof(digest), 0) == 0;
    if (hash)
        BCryptDestroyHash(hash);
    if (algorithm)
        BCryptCloseAlgorithmProvider(algorithm, 0);
    if (!okay)
        return false;
    std::ostringstream encoded;
    for (auto b : digest)
        encoded << std::hex << std::setw(2) << std::setfill('0') << int(b);
    if (encoded.str() != std::string(expected, 64))
        return false;
    if (!directory.empty())
    {
        std::error_code error;
        std::filesystem::create_directories(directory, error);
        if (error)
            return false;
    }
    auto destination = directory / L"GreaterArt.msi";
    // Reserve the file and reject a collision in one operation.
    HANDLE output = CreateFileW(destination.c_str(), GENERIC_WRITE | DELETE, 0, nullptr, CREATE_NEW,
                                FILE_ATTRIBUTE_NORMAL | FILE_FLAG_OPEN_REPARSE_POINT, nullptr);
    if (output == INVALID_HANDLE_VALUE)
        return false;
    DWORD writtenBytes = 0;
    bool complete = WriteFile(output, data, size, &writtenBytes, nullptr) && writtenBytes == size &&
                    FlushFileBuffers(output);
    if (!complete)
    {
        // Clean up the file we opened, even if its path changes.
        FILE_DISPOSITION_INFO disposition{TRUE};
        SetFileInformationByHandle(output, FileDispositionInfo, &disposition, sizeof(disposition));
    }
    CloseHandle(output);
    if (!complete)
        return false;
    payload = destination;
    return true;
}
std::filesystem::path temporary()
{
    wchar_t root[MAX_PATH]{};
    GetTempPath(MAX_PATH, root);
    GUID id{};
    CoCreateGuid(&id);
    wchar_t guid[40]{};
    StringFromGUID2(id, guid, 40);
    return std::filesystem::path(root) / (std::wstring(L"GreaterArt-Install-") + guid);
}
void removePayload()
{
    if (payload.empty())
        return;
    std::error_code error;
    std::filesystem::remove(payload, error);
    std::filesystem::remove(payload.parent_path(), error);
}
void doInstall()
{
    MsiSetInternalUI(INSTALLUILEVEL_NONE, nullptr);
    result = MsiInstallProduct(payload.c_str(),
                               createShortcut ? L"CREATE_DESKTOP_SHORTCUT=1" : L"CREATE_DESKTOP_SHORTCUT=0");
}
void beginInstall()
{
    if (busy)
        return;
    createShortcut = SendMessage(shortcut, BM_GETCHECK, 0, 0) == BST_CHECKED;
    launchApp = SendMessage(launch, BM_GETCHECK, 0, 0) == BST_CHECKED;
    if (!extract(temporary()))
    {
        MessageBox(mainWindow, L"The embedded installer failed its integrity check. Nothing was installed.",
                   L"Greater Art", MB_OK | MB_ICONERROR);
        return;
    }
    busy = true;
    EnableWindow(installButton, FALSE);
    EnableWindow(shortcut, FALSE);
    EnableWindow(launch, FALSE);
    SetWindowText(installButton, L"Installing…");
    installer = std::thread([] {
        doInstall();
        PostMessage(mainWindow, WM_APP + 1, 0, 0);
    });
}
LRESULT CALLBACK procedure(HWND window, UINT message, WPARAM w, LPARAM l)
{
    switch (message)
    {
    case WM_CREATE: {
        shortcut =
            CreateWindow(L"BUTTON", L"Create a desktop shortcut", WS_CHILD | WS_VISIBLE | BS_AUTOCHECKBOX,
                         px(28), px(190), px(390), px(28), window, reinterpret_cast<HMENU>(INT_PTR(201)),
                         GetModuleHandle(nullptr), nullptr);
        launch =
            CreateWindow(L"BUTTON", L"Open Greater Art after installation",
                         WS_CHILD | WS_VISIBLE | BS_AUTOCHECKBOX, px(28), px(226), px(390), px(28), window,
                         reinterpret_cast<HMENU>(INT_PTR(202)), GetModuleHandle(nullptr), nullptr);
        installButton =
            CreateWindow(L"BUTTON", L"Install Greater Art", WS_CHILD | WS_VISIBLE | BS_DEFPUSHBUTTON, px(260),
                         px(304), px(200), px(44), window, reinterpret_cast<HMENU>(INT_PTR(IDOK)),
                         GetModuleHandle(nullptr), nullptr);
        for (auto control : {shortcut, launch, installButton})
            SendMessage(control, WM_SETFONT, reinterpret_cast<WPARAM>(bodyFont), TRUE);
        SendMessage(shortcut, BM_SETCHECK, BST_CHECKED, 0);
        SendMessage(launch, BM_SETCHECK, BST_CHECKED, 0);
        return 0;
    }
    case WM_PAINT: {
        PAINTSTRUCT paint{};
        auto dc = BeginPaint(window, &paint);
        RECT client{};
        GetClientRect(window, &client);
        FillRect(dc, &client, background);
        SetBkMode(dc, TRANSPARENT);
        SetTextColor(dc, RGB(235, 238, 236));
        SelectObject(dc, titleFont);
        RECT heading{px(28), px(28), px(470), px(72)};
        DrawText(dc, L"Install Greater Art", -1, &heading, DT_LEFT | DT_SINGLELINE);
        SelectObject(dc, bodyFont);
        RECT detail{px(28), px(88), px(466), px(170)};
        DrawText(dc,
                 L"Your local music. A proper desktop workspace.\nWindows-native playback. No VLC or bundled "
                 L"Java.\nFully offline installation; no administrator account needed.",
                 -1, &detail, DT_LEFT | DT_WORDBREAK);
        SetTextColor(dc, RGB(170, 179, 174));
        RECT version{px(28), px(280), px(230), px(350)};
        DrawText(dc, L"Desktop 0.1.21\nDevelopment edition", -1, &version, DT_LEFT | DT_WORDBREAK);
        EndPaint(window, &paint);
        return 0;
    }
    case WM_CTLCOLORBTN:
    case WM_CTLCOLORSTATIC:
        SetTextColor(reinterpret_cast<HDC>(w), RGB(235, 238, 236));
        SetBkColor(reinterpret_cast<HDC>(w), RGB(28, 28, 30));
        return reinterpret_cast<LRESULT>(background);
    case WM_COMMAND:
        if (LOWORD(w) == IDOK)
            beginInstall();
        return 0;
    case WM_APP + 1:
        if (installer.joinable())
            installer.join();
        busy = false;
        removePayload();
        if (result == ERROR_SUCCESS || result == ERROR_SUCCESS_REBOOT_REQUIRED)
        {
            if (launchApp)
            {
                auto executable = (installFolder() / L"GreaterArt.exe").wstring();
                ShellExecute(window, L"open", executable.c_str(), nullptr, nullptr, SW_SHOWNORMAL);
            }
            MessageBox(window, L"Greater Art is installed. Its Start menu shortcut is ready.", L"Greater Art",
                       MB_OK);
            DestroyWindow(window);
        }
        else
        {
            std::wstring error = L"Windows Installer returned error " + std::to_wstring(result) +
                                 L". Nothing was reported as successfully installed.";
            MessageBox(window, error.c_str(), L"Greater Art installation", MB_OK | MB_ICONERROR);
            EnableWindow(installButton, TRUE);
            EnableWindow(shortcut, TRUE);
            EnableWindow(launch, TRUE);
            SetWindowText(installButton, L"Retry installation");
        }
        return 0;
    case WM_CLOSE:
        if (!busy)
            DestroyWindow(window);
        return 0;
    case WM_DESTROY:
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProc(window, message, w, l);
}
} // namespace
int WINAPI wWinMain(HINSTANCE instance, HINSTANCE, PWSTR, int)
{
    SetProcessDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2);
    CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
    int argc = 0;
    auto argv = CommandLineToArgvW(GetCommandLine(), &argc);
    if (argv && argc >= 2 && wcscmp(argv[1], L"--extract-msi") == 0)
    {
        // Test extraction writes a fixed name in the caller's working directory.
        const bool okay = argc == 2 && localExtractionWorkingDirectory() && extract();
        LocalFree(argv);
        CoUninitialize();
        return okay ? 0 : 2;
    }
    if (argc == 2 && wcscmp(argv[1], L"--silent") == 0)
    {
        if (!extract(temporary()))
            return 2;
        doInstall();
        removePayload();
        LocalFree(argv);
        CoUninitialize();
        return result == ERROR_SUCCESS || result == ERROR_SUCCESS_REBOOT_REQUIRED ? 0 : int(result);
    }
    dpi = GetDpiForSystem();
    titleFont = CreateFont(-px(25), 0, 0, 0, FW_SEMIBOLD, FALSE, FALSE, FALSE, DEFAULT_CHARSET, 0, 0,
                           CLEARTYPE_QUALITY, 0, L"Segoe UI");
    bodyFont = CreateFont(-px(14), 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, 0, 0,
                          CLEARTYPE_QUALITY, 0, L"Segoe UI");
    background = CreateSolidBrush(RGB(28, 28, 30));
    WNDCLASS cls{};
    cls.lpfnWndProc = procedure;
    cls.hInstance = instance;
    cls.lpszClassName = L"GreaterArtSetup";
    cls.hCursor = LoadCursor(nullptr, IDC_ARROW);
    cls.hIcon = LoadIcon(instance, MAKEINTRESOURCE(101));
    cls.hbrBackground = background;
    RegisterClass(&cls);
    mainWindow = CreateWindowEx(0, cls.lpszClassName, L"Greater Art Setup",
                                WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_VISIBLE, CW_USEDEFAULT,
                                CW_USEDEFAULT, px(510), px(410), nullptr, nullptr, instance, nullptr);
    BOOL dark = TRUE;
    DwmSetWindowAttribute(mainWindow, 20, &dark, sizeof(dark));
    ShowWindow(mainWindow, SW_SHOWNORMAL);
    MSG message{};
    while (GetMessage(&message, nullptr, 0, 0) > 0)
    {
        TranslateMessage(&message);
        DispatchMessage(&message);
    }
    if (installer.joinable())
        installer.join();
    DeleteObject(titleFont);
    DeleteObject(bodyFont);
    DeleteObject(background);
    LocalFree(argv);
    CoUninitialize();
    return 0;
}
