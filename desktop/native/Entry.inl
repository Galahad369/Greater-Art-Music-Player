namespace
{
void writeIcon(const std::filesystem::path &file)
{
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
int tests(const std::filesystem::path &directory)
{
    std::filesystem::create_directories(directory);
    std::ofstream report(directory / L"unit-tests.txt");
    int passed = 0;
    int total = 0;
    auto expect = [&](bool condition, const char *name) {
        ++total;
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
    UserFeatures features;
    features.playlists.push_back({L"合唱", {canonicalPath((directory / L"scan/孤独毒毒.mp3").wstring())}});
    recordHistory(features, L"ignored", 42);
    expect(features.history.empty(), "history is off by default");
    features.historyEnabled = true;
    recordHistory(features, L"孤独毒毒", 42);
    expect(saveFeatures(directory, features), "atomic feature persistence");
    auto loadedFeatures = loadFeatures(directory);
    expect(loadedFeatures.playlists.size() == 1 && loadedFeatures.playlists[0].name == L"合唱" &&
               loadedFeatures.history.size() == 1,
           "Unicode playlists and opted-in history persist separately");
    features.historyEnabled = false;
    saveFeatures(directory, features);
    expect(loadFeatures(directory).history.empty(), "disabled history is not written");
    for (int i = 0; i < 2100; ++i)
    {
        features.historyEnabled = true;
        recordHistory(features, L"track", i);
    }
    expect(features.history.size() == 2000 && features.history[0].at == 100,
           "history bounded to 2000 entries");
    auto lrc = parseLyrics(L"[offset:100]\n[00:01.50][00:02.50]孤独毒毒\n[00:04.00]歌\n");
    expect(lrc.size() == 3 && std::abs(lrc[0].time - 1.6) < 1e-9 && lrc[1].text == L"孤独毒毒",
           "LRC Unicode multi-stamp and global offset");
    expect(activeLyric(lrc, 2.7) == 1 && activeLyric(lrc, .1) == -1, "lyrics follow playback timeline");
    expect(parseLyrics(L"[99:99.00]bad\n[NaN:02]bad").empty(), "invalid LRC timestamps rejected");
    expect(!localMediaPath(L"https://example.com/x.mp3") && !localMediaPath(L"\\\\server\\track.mp3") &&
               !localMediaPath(L"C:\\file.mp3:stream"),
           "network and alternate stream playlist paths rejected");
    expect(fromUtf8(toUtf8(L"歌𠮷")) == L"歌𠮷", "UTF-8 CJK and supplementary characters roundtrip");
    auto exportPath = directory / L"roundtrip.m3u8";
    std::error_code cleanup;
    std::filesystem::remove(exportPath, cleanup);
    expect(exportPlaylist(exportPath, features.playlists[0]) &&
               importPlaylist(exportPath).paths == features.playlists[0].paths,
           "portable local M3U8 roundtrip");
    expect(!exportPlaylist(exportPath, features.playlists[0]), "playlist export never overwrites");
    auto invalidExport = directory / L"invalid.m3u8";
    expect(!exportPlaylist(invalidExport, {L"Bad", {L"https://example.com/file.mp3"}}) &&
               !std::filesystem::exists(invalidExport),
           "invalid playlist export creates no partial file");
    auto cacheTest = directory / L"cache-prune";
    std::filesystem::create_directories(cacheTest);
    for (int i = 0; i < 130; ++i)
        std::ofstream(cacheTest / (L"wave-" + std::to_wstring(i) + L".bin"), std::ios::binary).put('x');
    std::ofstream(cacheTest / L"settings-v1.bin", std::ios::binary).put('s');
    std::ofstream(cacheTest / L"wave-not-a-cache.bin", std::ios::binary).put('u');
    pruneWaveCache(cacheTest);
    int cachedFiles = 0;
    for (auto &entry : std::filesystem::directory_iterator(cacheTest))
        if (entry.path().filename().wstring().rfind(L"wave-", 0) == 0)
            ++cachedFiles;
    expect(cachedFiles == 129, "waveform cache bounded to 128 generated entries");
    expect(std::filesystem::exists(cacheTest / L"settings-v1.bin") &&
               std::filesystem::exists(cacheTest / L"wave-not-a-cache.bin"),
           "cache eviction preserves settings and unrecognized files");
    Practice practice;
    practice.mark(1);
    practice.mark(3);
    expect(practice.shouldLoop(3) && !practice.shouldLoop(2.9), "A-B end loops to A");
    practice.mark(4);
    expect(practice.a < 0 && practice.b < 0, "A-B third tap clears markers");
    expect(correctionRate(.1) < 1 && correctionRate(-.1) > 1 && correctionRate(.005) == 1,
           "drift corrected in the right direction without tiny seeks");
    AudioFeatures primaryAudio, delayedAudio;
    for (int i = 0; i < 1600; ++i)
    {
        float value =
            float(std::exp(-std::pow((i - 180) / 8.0, 2)) + .7 * std::exp(-std::pow((i - 510) / 11.0, 2)) +
                  .8 * std::exp(-std::pow((i - 980) / 6.0, 2)));
        primaryAudio.energy.push_back(value);
        primaryAudio.attacks.push_back(i ? std::max(0.0f, value - primaryAudio.energy[i - 1]) : 0);
    }
    delayedAudio.energy.resize(1600);
    delayedAudio.attacks.resize(1600);
    for (int i = 75; i < 1600; ++i)
    {
        delayedAudio.energy[i] = primaryAudio.energy[i - 75] * .7f;
        delayedAudio.attacks[i] = primaryAudio.attacks[i - 75] * .7f;
    }
    auto match = matchAudio(primaryAudio, delayedAudio);
    expect(match.accepted && std::abs(match.offset - 1.5) < .025,
           "attack weighted alignment detects positive delay");
    auto inverse = matchAudio(delayedAudio, primaryAudio);
    expect(inverse.accepted && std::abs(inverse.offset + 1.5) < .025, "alignment lag sign is symmetric");
    AudioFeatures silence;
    silence.energy.resize(1600);
    silence.attacks.resize(1600);
    expect(!matchAudio(primaryAudio, silence).accepted, "silence does not invent an offset");
    for (int language = 1; language <= 5; ++language)
        expect(translate(L"Library", language) != L"Library", "library translated for supported language");
    original.stacks[0].tracks[0].offset = 1.25;
    saveSettings(directory, original);
    expect(loadSettings(directory).stacks[0].tracks[0].offset == 1.25, "saved Stack offset persists");
    expect(audioDecodeTest(directory.wstring()), "real offline decoder and 500ms waveform alignment");
    if (codecsAvailable())
    {
        CodecReader decoder;
        expect(decoder.open((directory / L"analysis-primary.wav").wstring(), [] { return false; }),
               "in-process bundled codec opens Unicode local WAV");
        std::vector<float> samples;
        double timestamp = 0;
        expect(decoder.read(samples, timestamp) && !samples.empty(), "bundled codec decodes real float PCM");
        expect(decoder.seek(2), "bundled codec seeks without network or subprocess");
        expect(codecLicense().find(L"LGPL") != std::wstring::npos, "bundled codec reports LGPL license");
        CodecReader rejected;
        expect(!rejected.open(L"https://example.com/song.mp3", [] { return false; }),
               "bundled decoder rejects URL sources");
    }
    report << "total=" << passed << "/" << total << "\n";
    return passed == total ? 0 : 2;
}
int audioPlaybackTests(const std::wstring &file, const std::filesystem::path &directory)
{
    std::filesystem::create_directories(directory);
    std::ofstream report(directory / L"audio-playback-tests.txt");
    int total = 0, passed = 0;
    auto expect = [&](bool condition, const char *name) {
        ++total;
        if (condition)
            ++passed;
        report << (condition ? "PASS " : "FAIL ") << name << "\n";
    };
    AudioFallback player(file, true);
    auto wait = [&](auto predicate, int timeout) {
        auto end = GetTickCount64() + timeout;
        do
        {
            auto s = player.state();
            if (predicate(s))
                return true;
            if (s.state == 7)
                return false;
            Sleep(10);
        } while (GetTickCount64() < end);
        return false;
    };
    expect(wait([](auto s) { return s.state == 4 && s.duration > 0; }, 5000),
           "bundled codec and WASAPI ready without auto-play");
    player.volume(.05);
    player.pause(false);
    expect(wait([](auto s) { return s.state == 3 && s.time > .2; }, 5000), "actual output clock advances");
    player.pause(true);
    wait([](auto s) { return s.state == 4; }, 2000);
    auto paused = player.state().time;
    Sleep(180);
    expect(std::abs(player.state().time - paused) < .02, "pause holds endpoint timeline");
    player.seek(2);
    expect(wait([](auto s) { return s.state == 4 && std::abs(s.time - 2) < .03; }, 3000),
           "seek while paused preserves requested start");
    player.pause(false);
    expect(wait([](auto s) { return s.state == 3 && s.time > 2.2; }, 3000), "seek resumes real audio");
    auto before = player.state().time;
    player.rate(2);
    Sleep(100);
    auto after = player.state().time;
    expect(after >= before && after - before < .5, "speed change does not rescale elapsed history");
    before = after;
    Sleep(650);
    after = player.state().time;
    expect(after - before > .8 && after - before < 1.65, "pitch-preserving 2x processes output continuously");
    player.rate(.5);
    Sleep(220);
    before = player.state().time;
    Sleep(650);
    after = player.state().time;
    expect(after - before > .15 && after - before < .6, "half-speed returns without timeline jump");
    expect(FAILED(player.rate(0)) && FAILED(player.seek(NAN)), "invalid playback commands rejected");
    player.pause(true);
    report << "total=" << passed << "/" << total << "\n";
    return passed == total ? 0 : 2;
}
int audioRoutingTest(const std::wstring &file, const std::filesystem::path &directory)
{
    std::filesystem::create_directories(directory);
    std::ofstream report(directory / L"audio-routing.txt");
    if (!mediaInitialize())
    {
        report << "FAIL initialize\n";
        return 2;
    }
    void *player = mediaCreate(true);
    bool passed = player && SUCCEEDED(mediaLoad(player, file)) && SUCCEEDED(mediaPlay(player));
    auto end = GetTickCount64() + 8000;
    MediaState state;
    while (passed && GetTickCount64() < end)
    {
        state = mediaState(player);
        if (state.state == 3 && state.time > .25)
            break;
        if (state.state == 7)
        {
            passed = false;
            break;
        }
        Sleep(15);
    }
    passed = passed && state.state == 3 && state.time > .25;
    if (passed)
    {
        mediaPause(player, true);
        Sleep(120);
        passed = mediaState(player).state == 4;
    }
    report << (passed ? "PASS" : "FAIL") << " media actor route; backend="
           << (state.bundledAudio ? "bundled-WASAPI" : "native-MediaFoundation") << " time=" << state.time
           << " state=" << state.state << "\n";
    if (player)
        mediaRelease(player);
    mediaShutdown();
    return passed ? 0 : 2;
}
} // namespace

int WINAPI wWinMain(HINSTANCE instance, HINSTANCE, PWSTR, int)
{
    SetProcessDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2);
    int argc = 0;
    auto argv = CommandLineToArgvW(GetCommandLine(), &argc);
    if (argc == 3 && wcscmp(argv[1], L"--write-icon") == 0)
    {
        writeIcon(argv[2]);
        LocalFree(argv);
        return 0;
    }
    if (argc == 3 && wcscmp(argv[1], L"--self-test") == 0)
    {
        int result = tests(argv[2]);
        LocalFree(argv);
        return result;
    }
    if (argc == 4 && wcscmp(argv[1], L"--verify-audio") == 0)
    {
        int result = audioPlaybackTests(argv[2], argv[3]);
        LocalFree(argv);
        return result;
    }
    if (argc == 4 && wcscmp(argv[1], L"--verify-routing") == 0)
    {
        int result = audioRoutingTest(argv[2], argv[3]);
        LocalFree(argv);
        return result;
    }
    winrt::init_apartment(winrt::apartment_type::single_threaded);
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
    if (argc == 4 && wcscmp(argv[1], L"--verify") == 0)
    {
        app.probe = true;
        app.probeFixture = argv[2];
        app.probeOutput = std::filesystem::absolute(argv[3]);
        std::filesystem::create_directories(app.probeOutput);
        app.dataDir = app.probeOutput / L"isolated-settings";
        app.probeAt = GetTickCount64();
    }
    app.settings = loadSettings(app.dataDir);
    std::filesystem::create_directories(app.dataDir);
    enableCrashRecord(app.dataDir.wstring());
    app.features = loadFeatures(app.dataDir);
    app.speed = app.features.speed;
    loadCustomBackground();
    HANDLE instanceMutex = nullptr;
    if (!app.probe)
    {
        instanceMutex = CreateMutex(nullptr, TRUE, L"Local\\GreaterArtDesktop.Native");
        if (GetLastError() == ERROR_ALREADY_EXISTS)
        {
            auto existing = FindWindow(L"GreaterArtPlayer", nullptr);
            if (!existing)
                existing = FindWindow(L"GreaterArtMain", nullptr);
            if (existing)
            {
                ShowWindow(existing, SW_RESTORE);
                SetForegroundWindow(existing);
            }
            if (instanceMutex)
                CloseHandle(instanceMutex);
            LocalFree(argv);
            winrt::uninit_apartment();
            return 0;
        }
    }
    app.dpi = GetDpiForSystem();
    fonts();
    theme();
    app.icon = LoadIcon(instance, MAKEINTRESOURCE(101));
    if (!mediaInitialize())
    {
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
    initializeSystemMedia();
    if (app.probe)
    {
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
    while (GetMessage(&message, nullptr, 0, 0) > 0)
    {
        if (!TranslateAccelerator(app.main, accelerator, &message))
        {
            TranslateMessage(&message);
            DispatchMessage(&message);
        }
    }
    app.scans.shutdown();
    DestroyAcceleratorTable(accelerator);
    for (auto font : {app.small, app.body, app.title, app.heading, app.icons})
        if (font)
            DeleteObject(font);
    if (app.editBrush)
        DeleteObject(app.editBrush);
    app.artwork.reset();
    app.customBackground.reset();
    app.assetResults.clear();
    removeSystemMedia();
    Gdiplus::GdiplusShutdown(gdiToken);
    winrt::uninit_apartment();
    LocalFree(argv);
    if (instanceMutex)
        CloseHandle(instanceMutex);
    return app.probeFailed ? 2 : 0;
}
