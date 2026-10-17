namespace
{
std::wstring chooseTextFile(bool save, const wchar_t *extension = L"m3u8")
{
    ComPtr<IFileDialog> dialog;
    auto id = save ? CLSID_FileSaveDialog : CLSID_FileOpenDialog;
    if (FAILED(CoCreateInstance(id, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&dialog))))
        return {};
    COMDLG_FILTERSPEC types[]{{L"UTF-8 playlists", L"*.m3u8;*.m3u"}, {L"All files", L"*.*"}};
    if (wcscmp(extension, L"png") == 0)
        types[0] = {L"Images", L"*.png;*.jpg;*.jpeg;*.bmp;*.gif"};
    dialog->SetFileTypes(2, types);
    dialog->SetDefaultExtension(extension);
    dialog->SetOptions(FOS_FORCEFILESYSTEM | FOS_NOCHANGEDIR |
                       (save ? FOS_PATHMUSTEXIST : FOS_FILEMUSTEXIST));
    if (FAILED(dialog->Show(app.main)))
        return {};
    ComPtr<IShellItem> item;
    if (FAILED(dialog->GetResult(&item)))
        return {};
    PWSTR path = nullptr;
    if (FAILED(item->GetDisplayName(SIGDN_FILESYSPATH, &path)))
        return {};
    std::wstring result(path);
    CoTaskMemFree(path);
    return localMediaPath(result) ? result : std::wstring{};
}
Playlist visiblePlaylist()
{
    Playlist list{app.activePlaylist.empty() ? L"Greater Art playlist" : app.activePlaylist, {}};
    for (auto index : app.filtered)
        list.paths.push_back(app.songs[index].path);
    return list;
}
void usePlaylist(const Playlist &list)
{
    app.activePlaylist = list.name;
    app.queue = list.paths;
    app.favoritesOnly = false;
    for (auto &path : list.paths)
    {
        if (std::any_of(app.songs.begin(), app.songs.end(), [&](auto &song) { return song.path == path; }))
            continue;
        std::error_code error;
        if (!std::filesystem::is_regular_file(path, error))
            continue;
        Song song;
        song.path = canonicalPath(path);
        song.title = titleOf(path);
        song.bytes = std::filesystem::file_size(path, error);
        song.format = std::filesystem::path(path).extension().wstring().substr(1);
        auto ext = lower(song.format);
        song.video = ext == L"mp4" || ext == L"mkv" || ext == L"webm" || ext == L"mov" || ext == L"avi";
        app.songs.push_back(song);
    }
    app.libraryScroll = 0;
    filter();
    std::stable_sort(app.filtered.begin(), app.filtered.end(), [](int a, int b) {
        return std::find(app.queue.begin(), app.queue.end(), app.songs[a].path) <
               std::find(app.queue.begin(), app.queue.end(), app.songs[b].path);
    });
}
void playlistMenu()
{
    HMENU menu = CreatePopupMenu();
    AppendMenu(menu, MF_STRING, 1, L"All songs");
    AppendMenu(menu, MF_STRING, 2, L"Import local M3U8…");
    AppendMenu(menu, MF_STRING, 3, L"Save visible list…");
    AppendMenu(menu, MF_STRING, 4, L"Export visible list…");
    AppendMenu(menu, MF_STRING, 5, L"Play visible list");
    AppendMenu(menu, MF_SEPARATOR, 0, nullptr);
    for (std::size_t i = 0; i < app.features.playlists.size(); ++i)
        AppendMenu(menu, MF_STRING, 100 + UINT(i), app.features.playlists[i].name.c_str());
    AppendMenu(menu, MF_SEPARATOR, 0, nullptr);
    AppendMenu(menu, MF_STRING, 6, L"Delete saved playlist…");
    POINT p{};
    GetCursorPos(&p);
    int choice = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, p.x, p.y, 0, app.main, nullptr);
    DestroyMenu(menu);
    if (choice == 1)
    {
        app.activePlaylist.clear();
        app.queue.clear();
        filter();
    }
    if (choice == 2)
    {
        auto file = chooseTextFile(false);
        if (!file.empty())
        {
            auto list = importPlaylist(file);
            if (list.paths.empty())
            {
                toast(L"This playlist contains no readable local media.");
                return;
            }
            if (app.features.playlists.size() >= 1000)
            {
                toast(L"Playlist limit reached.");
                return;
            }
            app.features.playlists.push_back(list);
            persist();
            usePlaylist(list);
        }
    }
    if (choice == 3 || choice == 4)
    {
        auto file = chooseTextFile(true);
        if (!file.empty())
        {
            auto list = visiblePlaylist();
            list.name = std::filesystem::path(file).stem().wstring();
            if (!exportPlaylist(file, list))
            {
                toast(L"Choose a new filename; existing exports are never overwritten.");
                return;
            }
            if (choice == 3)
            {
                app.features.playlists.push_back(list);
                persist();
            }
            toast(L"Playlist exported as UTF-8 M3U8.");
        }
    }
    if (choice == 5 && !app.filtered.empty())
        playSong(app.songs[app.filtered[0]]);
    if (choice >= 100 && choice < 100 + int(app.features.playlists.size()))
        usePlaylist(app.features.playlists[choice - 100]);
    if (choice == 6)
    {
        menu = CreatePopupMenu();
        for (std::size_t i = 0; i < app.features.playlists.size(); ++i)
            AppendMenu(menu, MF_STRING, 100 + UINT(i), app.features.playlists[i].name.c_str());
        int item = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, p.x, p.y, 0, app.main, nullptr);
        DestroyMenu(menu);
        if (item >= 100 && item < 100 + int(app.features.playlists.size()) &&
            MessageBox(app.main, L"Delete this saved playlist? Media files are kept.", L"Delete playlist",
                       MB_YESNO | MB_DEFBUTTON2) == IDYES)
        {
            app.features.playlists.erase(app.features.playlists.begin() + item - 100);
            persist();
        }
    }
}
void historyMenu()
{
    HMENU menu = CreatePopupMenu();
    AppendMenu(menu, MF_STRING | (app.features.historyEnabled ? MF_CHECKED : 0), 1,
               L"Record local history (off by default)");
    AppendMenu(menu, MF_STRING, 2, L"Burn all history…");
    AppendMenu(menu, MF_SEPARATOR, 0, nullptr);
    int count = (std::min)(50, int(app.features.history.size()));
    for (int i = 0; i < count; ++i)
    {
        auto &entry = app.features.history[app.features.history.size() - 1 - i];
        AppendMenu(menu, MF_STRING, 100 + i, titleOf(entry.path).c_str());
    }
    POINT p{};
    GetCursorPos(&p);
    int item = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, p.x, p.y, 0, app.main, nullptr);
    DestroyMenu(menu);
    if (item == 1)
    {
        app.features.historyEnabled = !app.features.historyEnabled;
        if (!app.features.historyEnabled)
            app.features.history.clear();
        persist();
    }
    if (item == 2 &&
        MessageBox(app.main, L"Permanently clear the local playback history? Your media is kept.",
                   L"Burn history", MB_YESNO | MB_DEFBUTTON2) == IDYES)
    {
        app.features.history.clear();
        persist();
    }
    if (item >= 100 && item < 100 + count)
    {
        auto path = app.features.history[app.features.history.size() - 1 - (item - 100)].path;
        auto song = std::find_if(app.songs.begin(), app.songs.end(), [&](auto &s) { return s.path == path; });
        if (song != app.songs.end())
            playSong(*song);
        else
            toast(L"This file is no longer in the Library.");
    }
}
LRESULT CALLBACK DetailsProc(HWND window, UINT message, WPARAM w, LPARAM l)
{
    if (message == WM_CREATE)
    {
        CreateWindowEx(WS_EX_CLIENTEDGE, L"EDIT", L"",
                       WS_CHILD | WS_VISIBLE | WS_VSCROLL | WS_HSCROLL | ES_MULTILINE | ES_READONLY, 0, 0, 1,
                       1, window, reinterpret_cast<HMENU>(501), GetModuleHandle(nullptr), nullptr);
        SendMessage(GetDlgItem(window, 501), WM_SETFONT, reinterpret_cast<WPARAM>(app.body), TRUE);
        return 0;
    }
    if (message == WM_SIZE)
    {
        MoveWindow(GetDlgItem(window, 501), 0, 0, LOWORD(l), HIWORD(l), TRUE);
        return 0;
    }
    if (message == WM_DESTROY)
    {
        app.details = nullptr;
        app.lyricsPanel = false;
        return 0;
    }
    if (message == WM_CTLCOLOREDIT || message == WM_CTLCOLORSTATIC)
    {
        SetTextColor(reinterpret_cast<HDC>(w), colors.text);
        SetBkColor(reinterpret_cast<HDC>(w), colors.panel);
        return reinterpret_cast<LRESULT>(app.editBrush);
    }
    return DefWindowProc(window, message, w, l);
}
void showDetails(const std::wstring &title, const std::wstring &content)
{
    if (!app.details)
    {
        WNDCLASS cls{};
        cls.lpfnWndProc = DetailsProc;
        cls.hInstance = GetModuleHandle(nullptr);
        cls.lpszClassName = L"GreaterArtDetails";
        cls.hCursor = LoadCursor(nullptr, IDC_ARROW);
        RegisterClass(&cls);
        app.details = CreateWindowEx(0, cls.lpszClassName, title.c_str(), WS_OVERLAPPEDWINDOW | WS_VISIBLE,
                                     CW_USEDEFAULT, CW_USEDEFAULT, px(640), px(600),
                                     app.player ? app.player : app.main, nullptr, cls.hInstance, nullptr);
    }
    SetWindowText(app.details, title.c_str());
    SetWindowText(GetDlgItem(app.details, 501), content.c_str());
    ShowWindow(app.details, SW_RESTORE);
    SetForegroundWindow(app.details);
}
std::wstring diagnosticText()
{
    std::wostringstream out;
    out << L"Greater Art Desktop " << VERSION << L"\r\nPresentation: "
        << (app.mini     ? L"Mini"
            : app.player ? L"Now Playing"
                         : L"Library")
        << L"\r\nPlayback: " << app.playback.state << L"\r\nSource: " << app.playback.width << L" × "
        << app.playback.height << L"\r\nRendered frames: " << app.playback.frames << L"\r\nPosition: "
        << formatTime(app.playback.time) << L" / " << formatTime(app.playback.duration) << L"\r\nSpeed: "
        << app.speed << L"×\r\nVoices: " << app.slots.size()
        << L" / 8\r\nOffline. No source-resolution, bitrate or FPS limit.\r\n";
    if (app.stackOutput)
    {
        out << L"\r\nStack audio: one PCM bus / one WASAPI clock\r\nEndpoint: " << app.playback.pcmRate
            << L" Hz / " << app.playback.pcmChannels << L" channels\r\nUnderruns: "
            << app.playback.pcmUnderruns
            << L"\r\nFractional offsets precede mixing. Musical matching is confidence-gated.\r\n";
        for (std::size_t i = 0; i < app.slots.size(); ++i)
            out << L"Voice " << i + 1 << L": offset=" << app.slots[i].offset << L"s\r\n";
        return out.str();
    }
    for (std::size_t i = 0; i < app.slots.size(); ++i)
    {
        auto &slot = app.slots[i];
        auto state = mediaState(slot.player);
        out << L"\r\nVoice " << i + 1 << L": offset=" << slot.offset << L"s; drift="
            << state.time - app.playback.time - slot.offset << L"s; state=" << state.state;
    }
    return out.str();
}
void toolsMenu(HWND owner)
{
    HMENU menu = CreatePopupMenu();
    const wchar_t *items[]{L"A–B: mark A / mark B / clear",
                           L"Sleep timer…",
                           L"Local synchronized lyrics",
                           L"Media information",
                           L"Shuffle",
                           L"Resume previous track",
                           L"Black disc for audio",
                           L"Background / Ambient…",
                           L"Language…",
                           L"High contrast",
                           L"Developer diagnostics",
                           L"Developer mode",
                           L"Copy current file path",
                           L"Export current queue…",
                           L"Clear all audio offsets",
                           L"Nodes controls…",
                           L"Background sizing: Crop / Fit",
                           L"Light / dark theme",
                           L"Font…",
                           L"Quit Greater Art"};
    for (int i = 0; i < 20; ++i)
        AppendMenu(menu, MF_STRING, i + 1, translate(items[i], app.features.language).c_str());
    POINT p{};
    GetCursorPos(&p);
    int choice = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, p.x, p.y, 0, owner, nullptr);
    DestroyMenu(menu);
    switch (choice)
    {
    case 1:
        if (!app.slots.empty())
            app.practice.mark(app.playback.time);
        break;
    case 2: {
        menu = CreatePopupMenu();
        int minutes[]{0, 5, 15, 30, 60, 90};
        for (int i = 0; i < 6; ++i)
            AppendMenu(menu, MF_STRING, i + 1,
                       i == 0 ? L"Off" : (std::to_wstring(minutes[i]) + L" minutes").c_str());
        int value = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, p.x, p.y, 0, owner, nullptr);
        DestroyMenu(menu);
        if (value)
            app.sleepUntil = minutes[value - 1] ? GetTickCount64() + minutes[value - 1] * 60000 : 0;
        break;
    }
    case 3: {
        std::wstring lyrics;
        for (auto &line : app.lyrics)
            lyrics += formatTime(line.time) + L"  " + line.text + L"\r\n";
        showDetails(
            L"Local lyrics",
            lyrics.empty()
                ? L"Place a UTF-8 .lrc file beside your media with the same filename. No online lookup."
                : lyrics);
        app.lyricsPanel = !app.lyrics.empty();
        break;
    }
    case 4:
        if (!app.slots.empty())
            showDetails(L"Media information", app.slots[0].song.title + L"\r\n" + app.slots[0].song.path +
                                                  L"\r\n" + app.slots[0].song.format + L" · " +
                                                  bytes(app.slots[0].song) + L"\r\n" + diagnosticText());
        break;
    case 5:
        app.features.shuffle = !app.features.shuffle;
        break;
    case 6:
        app.features.resume = !app.features.resume;
        break;
    case 7:
        app.features.disc = !app.features.disc;
        break;
    case 8: {
        menu = CreatePopupMenu();
        const wchar_t *names[]{L"Solid", L"Classic Ambient — video / artwork",
                               L"Adaptive Ambient — video / artwork", L"Custom image…"};
        for (int i = 0; i < 4; ++i)
            AppendMenu(menu, MF_STRING, i + 1, translate(names[i], app.features.language).c_str());
        int value = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, p.x, p.y, 0, owner, nullptr);
        DestroyMenu(menu);
        if (value)
        {
            app.features.background = value - 1;
            if (value == 4)
            {
                auto image = chooseTextFile(false, L"png");
                if (!image.empty())
                {
                    app.features.customImage = image;
                    loadCustomBackground();
                }
            }
        }
        break;
    }
    case 9: {
        menu = CreatePopupMenu();
        const wchar_t *names[]{L"English", L"繁體中文", L"日本語", L"Deutsch", L"Français", L"廣東話"};
        for (int i = 0; i < 6; ++i)
            AppendMenu(menu, MF_STRING, i + 1, names[i]);
        int value = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, p.x, p.y, 0, owner, nullptr);
        DestroyMenu(menu);
        if (value)
            app.features.language = value - 1;
        break;
    }
    case 10:
        app.features.highContrast = !app.features.highContrast;
        theme();
        break;
    case 11:
        showDetails(L"Developer diagnostics", diagnosticText());
        break;
    case 12:
        app.features.developer = !app.features.developer;
        toast(app.features.developer
                  ? L"Developer mode: Ctrl+click any drawn control to inspect it without activating it."
                  : L"Developer mode off.");
        break;
    case 13:
        if (!app.slots.empty())
            clipboard(app.slots[0].song.path);
        break;
    case 14: {
        auto file = chooseTextFile(true);
        if (!file.empty() && !exportPlaylist(file, visiblePlaylist()))
            toast(L"Export failed; choose a new filename.");
        break;
    }
    case 15:
        for (auto &slot : app.slots)
            slot.offset = 0;
        seek(app.playback.time);
        break;
    case 16: {
        menu = CreatePopupMenu();
        AppendMenu(menu, MF_STRING, 1,
                   (L"Neighbors: " + std::to_wstring(app.features.graphNeighbors)).c_str());
        AppendMenu(
            menu, MF_STRING, 2,
            (L"Minimum link strength: " + std::to_wstring(app.features.graphThreshold) + L"%").c_str());
        AppendMenu(menu, MF_STRING, 3, L"Reset camera");
        AppendMenu(menu, MF_STRING, 4, L"Rebuild / reveal graph");
        int value = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, p.x, p.y, 0, owner, nullptr);
        DestroyMenu(menu);
        if (value == 1)
        {
            app.features.graphNeighbors =
                app.features.graphNeighbors == 8 ? 2 : app.features.graphNeighbors + 2;
            scan();
        }
        if (value == 2)
        {
            app.features.graphThreshold = (app.features.graphThreshold + 10) % 60;
            scan();
        }
        if (value == 3)
        {
            fitGraph();
        }
        if (value == 4)
        {
            restartGraph(true);
        }
        break;
    }
    case 17:
        app.features.scale = app.features.scale ? 0 : 1;
        toast(app.features.scale ? L"Background: Fit" : L"Background: Crop");
        break;
    case 18:
        app.features.light = !app.features.light;
        theme();
        break;
    case 19: {
        menu = CreatePopupMenu();
        const wchar_t *names[]{L"System", L"Sans serif", L"Serif", L"Monospace"};
        for (int i = 0; i < 4; ++i)
            AppendMenu(menu, MF_STRING, i + 1, names[i]);
        int value = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, p.x, p.y, 0, owner, nullptr);
        DestroyMenu(menu);
        if (value)
        {
            app.features.font = value - 1;
            fonts();
            SendMessage(app.search, WM_SETFONT, reinterpret_cast<WPARAM>(app.body), TRUE);
            SendMessage(app.nodeSearch, WM_SETFONT, reinterpret_cast<WPARAM>(app.body), TRUE);
        }
        break;
    }
    case 20:
        app.exiting = true;
        PostMessage(app.main, WM_CLOSE, 0, 0);
        break;
    }
    persist();
    invalidate();
}
} // namespace
