namespace {
void writeIcon(const std::filesystem::path &file) {
    int oldDpi = app.dpi;
    app.dpi = 96;
    constexpr int size = 64;
    BITMAPINFO info{};
    info.bmiHeader.biSize = 40;
    info.bmiHeader.biWidth = size;
    info.bmiHeader.biHeight = -size;
    info.bmiHeader.biPlanes = 1;
    info.bmiHeader.biBitCount = 32;
    info.bmiHeader.biCompression = BI_RGB;
    void *pixels = nullptr;
    HDC dc = CreateCompatibleDC(nullptr);
    HBITMAP bitmap = CreateDIBSection(dc, &info, DIB_RGB_COLORS, &pixels, nullptr, 0);
    auto old = SelectObject(dc, bitmap);
    mark(dc, {0, 0, size, size});
    GdiFlush();
    auto bytes = static_cast<unsigned char *>(pixels);
    for (int i = 0; i < size * size; ++i)
        bytes[i * 4 + 3] = 255;
    std::ofstream output(file, std::ios::binary);
    std::uint16_t header[]{0, 1, 1};
    output.write(reinterpret_cast<char *>(header), 6);
    unsigned char dimensions[]{64, 64, 0, 0};
    output.write(reinterpret_cast<char *>(dimensions), 4);
    std::uint16_t fields[]{1, 32};
    output.write(reinterpret_cast<char *>(fields), 4);
    std::uint32_t count = 40 + size * size * 4 + size * 8, offset = 22;
    output.write(reinterpret_cast<char *>(&count), 4);
    output.write(reinterpret_cast<char *>(&offset), 4);
    auto dib = info.bmiHeader;
    dib.biHeight = size * 2;
    output.write(reinterpret_cast<char *>(&dib), 40);
    for (int y = size - 1; y >= 0; --y)
        output.write(reinterpret_cast<char *>(pixels) + y * size * 4, size * 4);
    std::array<char, size * 8> mask{};
    output.write(mask.data(), mask.size());
    SelectObject(dc, old);
    DeleteObject(bitmap);
    DeleteDC(dc);
    app.dpi = oldDpi;
}
int tests(const std::filesystem::path &directory) {
    std::filesystem::create_directories(directory);
    std::ofstream report(directory / L"unit-tests.txt");
    int passed = 0;
    auto expect = [&](bool condition, const char *name) {
        report << (condition ? "PASS " : "FAIL ") << name << "\n";
        if (condition)
            ++passed;
    };
    expect(similarity(filenameFeatures(L"Caravan.mp3"), filenameFeatures(L"Gambling.mp4")) > 0,
           "shared-letter similarity");
    expect(similarity(filenameFeatures(L"孤独毒毒.mp4"), filenameFeatures(L"孤独毒毒.mp3")) == 1,
           "CJK extension removal");
    expect(filenameFeatures(L"ビターチョコデコレーション.mp4").words.size() == 1, "continuous Japanese word");
    expect(mixedGain(80, 50, false, false, false) == 40, "master applies to slot gain");
    expect(mixedGain(80, 50, false, false, true) == 0, "solo suppresses other voices");
    expect(mixedGain(80, 50, true, true, true) == 0, "mute wins over solo");
    expect(formatTime(4200) == L"1:10:00", "hour duration format");
    Settings original;
    original.roots = {(directory / L"歌").wstring()};
    original.favorites.insert((directory / L"孤独毒毒.mp4").wstring());
    original.stacks.push_back({L"合唱", {{(directory / L"歌/cover.mp4").wstring(), 57, true, false}}});
    expect(saveSettings(directory, original), "atomic settings save");
    auto restored = loadSettings(directory);
    expect(restored.roots == original.roots && restored.favorites == original.favorites,
           "Unicode root and favorite persistence");
    expect(restored.stacks.size() == 1 && restored.stacks[0].tracks[0].gain == 57 &&
               restored.stacks[0].tracks[0].mute,
           "Stack persistence separate from Library");
    auto chosen = directory / L"scan";
    std::filesystem::create_directories(chosen / L"nested");
    std::ofstream(chosen / L"孤独毒毒.mp3").put('x');
    std::ofstream(chosen / L"nested/孤独毒毒.mp3").put('x');
    std::ofstream(directory / L"outside.mp3").put('x');
    auto scanned = scanLibrary({chosen.wstring(), chosen.wstring()}, [] { return false; });
    expect(scanned.size() == 2, "root scope and duplicate filenames");
    expect(scanLibrary({chosen.wstring()}, [] { return true; }).empty(), "scan cancellation");
    auto graph = buildGraph(scanned, [] { return false; });
    expect(graph.points.size() == 2 && !graph.edges.empty(), "real sparse graph");
    expect(saveGraphCache(directory, scanned, graph), "atomic graph cache save");
    Graph cached;
    expect(loadGraphCache(directory, scanned, cached) && cached.edges.size() == graph.edges.size(),
           "graph cache hit");
    auto changed = scanned;
    changed[0].modified++;
    expect(!loadGraphCache(directory, changed, cached), "graph cache invalidation by file change");
    expect(canonicalPath((directory / L"scan/../scan/孤独毒毒.mp3").wstring()) ==
               canonicalPath((directory / L"scan/孤独毒毒.mp3").wstring()),
           "canonical presentation identity");
    std::ofstream(directory / L"settings-v1.bin", std::ios::binary | std::ios::trunc) << "malformed";
    expect(loadSettings(directory).roots.empty(), "malformed settings fail safely");
    report << "total=" << passed << "/18\n";
    return passed == 18 ? 0 : 2;
}
} // namespace

int WINAPI wWinMain(HINSTANCE instance, HINSTANCE, PWSTR, int) {
    SetProcessDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2);
    int argc = 0;
    auto argv = CommandLineToArgvW(GetCommandLine(), &argc);
    if (argc == 3 && wcscmp(argv[1], L"--write-icon") == 0) {
        writeIcon(argv[2]);
        LocalFree(argv);
        return 0;
    }
    if (argc == 3 && wcscmp(argv[1], L"--self-test") == 0) {
        int result = tests(argv[2]);
        LocalFree(argv);
        return result;
    }
    CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
    INITCOMMONCONTROLSEX controls{sizeof(controls), ICC_STANDARD_CLASSES};
    InitCommonControlsEx(&controls);
    ULONG_PTR gdiToken = 0;
    Gdiplus::GdiplusStartupInput input;
    Gdiplus::GdiplusStartup(&gdiToken, &input, nullptr);
    PWSTR data = nullptr;
    SHGetKnownFolderPath(FOLDERID_LocalAppData, KF_FLAG_CREATE, nullptr, &data);
    app.dataDir = std::filesystem::path(data ? data : L".") / L"GreaterArtDesktopNative";
    if (data)
        CoTaskMemFree(data);
    if (argc == 4 && wcscmp(argv[1], L"--verify") == 0) {
        app.probe = true;
        app.probeFixture = argv[2];
        app.probeOutput = std::filesystem::absolute(argv[3]);
        std::filesystem::create_directories(app.probeOutput);
        app.dataDir = app.probeOutput / L"isolated-settings";
        app.probeAt = GetTickCount64();
    }
    app.settings = loadSettings(app.dataDir);
    HANDLE instanceMutex = nullptr;
    if (!app.probe) {
        instanceMutex = CreateMutex(nullptr, TRUE, L"Local\\GreaterArtDesktop.Native");
        if (GetLastError() == ERROR_ALREADY_EXISTS) {
            auto existing = FindWindow(L"GreaterArtPlayer", nullptr);
            if (!existing)
                existing = FindWindow(L"GreaterArtMain", nullptr);
            if (existing) {
                ShowWindow(existing, SW_RESTORE);
                SetForegroundWindow(existing);
            }
            if (instanceMutex)
                CloseHandle(instanceMutex);
            LocalFree(argv);
            CoUninitialize();
            return 0;
        }
    }
    app.dpi = GetDpiForSystem();
    fonts();
    theme();
    app.icon = LoadIcon(instance, MAKEINTRESOURCE(101));
    if (!mediaInitialize()) {
        MessageBox(nullptr,
                   L"Windows Media Foundation is unavailable. On Windows N, enable the Windows Media Feature "
                   L"Pack. No external-player fallback is used.",
                   L"Greater Art", MB_OK | MB_ICONERROR);
        LocalFree(argv);
        return 2;
    }
    auto registerWindow = [&](const wchar_t *name, WNDPROC procedure) {
        WNDCLASSEX cls{sizeof(WNDCLASSEX)};
        cls.style = CS_DBLCLKS;
        cls.lpfnWndProc = procedure;
        cls.hInstance = instance;
        cls.hCursor = LoadCursor(nullptr, IDC_ARROW);
        cls.hIcon = app.icon;
        cls.hIconSm = app.icon;
        cls.lpszClassName = name;
        RegisterClassEx(&cls);
    };
    registerWindow(L"GreaterArtMain", WindowProc);
    registerWindow(L"GreaterArtPlayer", WindowProc);
    registerWindow(L"GreaterArtMini", WindowProc);
    registerWindow(L"GreaterArtPreview", PreviewProc);
    registerWindow(L"GreaterArtPicker", PickerProc);
    registerWindow(L"GreaterArtName", NameProc);
    MONITORINFO monitor{sizeof(MONITORINFO)};
    GetMonitorInfo(MonitorFromPoint({0, 0}, MONITOR_DEFAULTTOPRIMARY), &monitor);
    int width = std::min(px(1400), int(monitor.rcWork.right - monitor.rcWork.left) - 32),
        height = std::min(px(860), int(monitor.rcWork.bottom - monitor.rcWork.top) - 32);
    app.main = CreateWindowEx(0, L"GreaterArtMain", L"Greater Art",
                              WS_OVERLAPPEDWINDOW | WS_VISIBLE | WS_CLIPCHILDREN, monitor.rcWork.left + 16,
                              monitor.rcWork.top + 16, width, height, nullptr, nullptr, instance, nullptr);
    BOOL dark = TRUE;
    DwmSetWindowAttribute(app.main, 20, &dark, sizeof(dark));
    layout();
    if (app.probe) {
        app.settings.roots = {std::filesystem::path(app.probeFixture).parent_path().wstring()};
        Song song;
        song.path = app.probeFixture;
        song.title = titleOf(song.path);
        song.format = L"MP4";
        song.video = true;
        std::error_code error;
        song.bytes = std::filesystem::file_size(song.path, error);
        playSong(song);
    }
    scan();
    ACCEL keys[]{{BYTE(FCONTROL | FVIRTKEY), VK_SPACE, 401},
                 {BYTE(FCONTROL | FVIRTKEY), 'O', 402},
                 {BYTE(FCONTROL | FVIRTKEY), VK_OEM_COMMA, 403}};
    HACCEL accelerator = CreateAcceleratorTable(keys, 3);
    MSG message{};
    while (GetMessage(&message, nullptr, 0, 0) > 0) {
        if (!TranslateAccelerator(app.main, accelerator, &message)) {
            TranslateMessage(&message);
            DispatchMessage(&message);
        }
    }
    for (auto &worker : app.scans)
        if (worker.joinable())
            worker.join();
    DestroyAcceleratorTable(accelerator);
    for (auto font : {app.small, app.body, app.title, app.heading, app.icons})
        if (font)
            DeleteObject(font);
    if (app.editBrush)
        DeleteObject(app.editBrush);
    Gdiplus::GdiplusShutdown(gdiToken);
    CoUninitialize();
    LocalFree(argv);
    if (instanceMutex)
        CloseHandle(instanceMutex);
    return app.probeFailed ? 2 : 0;
}

