namespace
{
void capture(HWND window, const std::filesystem::path &file)
{
    RECT r{};
    GetWindowRect(window, &r);
    BOOL top = (GetWindowLongPtr(window, GWL_EXSTYLE) & WS_EX_TOPMOST) != 0;
    ShowWindow(window, SW_SHOW);
    SetWindowPos(window, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE | SWP_SHOWWINDOW);
    UpdateWindow(window);
    DwmFlush();
    HDC screen = GetDC(window), memory = CreateCompatibleDC(screen);
    HBITMAP bitmap = CreateCompatibleBitmap(screen, r.right - r.left, r.bottom - r.top);
    auto old = SelectObject(memory, bitmap);
    // Capture the owned window, not desktop pixels that may contain another app.
    if (!PrintWindow(window, memory, 2))
    {
        SelectObject(memory, old);
        DeleteObject(bitmap);
        DeleteDC(memory);
        ReleaseDC(window, screen);
        return;
    }
    UINT count = 0, size = 0;
    Gdiplus::GetImageEncodersSize(&count, &size);
    std::vector<unsigned char> bytes(size);
    auto encoders = reinterpret_cast<Gdiplus::ImageCodecInfo *>(bytes.data());
    Gdiplus::GetImageEncoders(count, size, encoders);
    CLSID encoder{};
    for (UINT i = 0; i < count; ++i)
        if (wcscmp(encoders[i].MimeType, L"image/png") == 0)
            encoder = encoders[i].Clsid;
    Gdiplus::Bitmap image(bitmap, nullptr);
    image.Save(file.c_str(), &encoder, nullptr);
    SelectObject(memory, old);
    DeleteObject(bitmap);
    DeleteDC(memory);
    ReleaseDC(window, screen);
    if (!top)
        SetWindowPos(window, HWND_NOTOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
}
void setFullscreen(bool value)
{
    if (!app.player)
        return;
    if (value == app.fullscreen)
        return;
    app.fullscreen = value;
    if (value)
    {
        GetWindowPlacement(app.player, &app.placement);
        SetWindowLongPtr(app.player, GWL_STYLE, WS_POPUP | WS_VISIBLE | WS_CLIPCHILDREN);
        MONITORINFO monitor{sizeof(MONITORINFO)};
        GetMonitorInfo(MonitorFromWindow(app.player, MONITOR_DEFAULTTONEAREST), &monitor);
        SetWindowPos(app.player, HWND_TOP, monitor.rcMonitor.left, monitor.rcMonitor.top,
                     monitor.rcMonitor.right - monitor.rcMonitor.left,
                     monitor.rcMonitor.bottom - monitor.rcMonitor.top, SWP_FRAMECHANGED);
    }
    else
    {
        SetWindowLongPtr(app.player, GWL_STYLE, WS_OVERLAPPEDWINDOW | WS_VISIBLE | WS_CLIPCHILDREN);
        SetWindowPlacement(app.player, &app.placement);
        SetWindowPos(app.player, nullptr, 0, 0, 0, 0,
                     SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER);
    }
    layout();
    invalidate();
}
void closePlayer()
{
    if (!app.player)
        return;
    auto window = app.player;
    app.fullscreen = false;
    app.player = nullptr;
    positionPreview(app.main, {64, app.clientH - 104, 128, 80});
    DestroyWindow(window);
    if (IsIconic(app.main))
        miniPlayer();
    else
    {
        layout();
        invalidate();
    }
}
void openPlayer()
{
    if (app.slots.empty())
        return;
    if (app.player)
    {
        ShowWindow(app.player, SW_RESTORE);
        SetForegroundWindow(app.player);
        return;
    }
    auto previousMini = app.mini;
    app.mini = nullptr;
    MONITORINFO monitor{sizeof(MONITORINFO)};
    GetMonitorInfo(MonitorFromWindow(app.main, MONITOR_DEFAULTTONEAREST), &monitor);
    int w = std::min(px(960), int(monitor.rcWork.right - monitor.rcWork.left) - 24),
        h = std::min(px(820), int(monitor.rcWork.bottom - monitor.rcWork.top) - 24);
    app.player = CreateWindowEx(0, L"GreaterArtPlayer", L"Greater Art · Now Playing",
                                WS_OVERLAPPEDWINDOW | WS_VISIBLE | WS_CLIPCHILDREN,
                                monitor.rcWork.left + (monitor.rcWork.right - monitor.rcWork.left - w) / 2,
                                monitor.rcWork.top + (monitor.rcWork.bottom - monitor.rcWork.top - h) / 2, w,
                                h, nullptr, nullptr, GetModuleHandle(nullptr), nullptr);
    BOOL dark = TRUE;
    DwmSetWindowAttribute(app.player, 20, &dark, sizeof(dark));
    SendMessage(app.player, WM_SETICON, ICON_BIG, reinterpret_cast<LPARAM>(app.icon));
    layout();
    if (previousMini)
        DestroyWindow(previousMini);
    SetForegroundWindow(app.player);
}
void miniPlayer()
{
    if (app.slots.empty())
        return;
    auto old = app.player;
    app.player = nullptr;
    app.fullscreen = false;
    if (!app.mini)
    {
        MONITORINFO monitor{sizeof(MONITORINFO)};
        GetMonitorInfo(MonitorFromWindow(app.main, MONITOR_DEFAULTTONEAREST), &monitor);
        auto metrics = compactMetrics();
        int size = int(metrics.x), mediaHeight = int(metrics.y);
        app.mini =
            CreateWindowEx(WS_EX_TOPMOST | WS_EX_TOOLWINDOW, L"GreaterArtMini", L"Greater Art Mini",
                           WS_POPUP | WS_VISIBLE | WS_CLIPCHILDREN, monitor.rcWork.right - px(size) - 16,
                           monitor.rcWork.bottom - px(mediaHeight + 36), px(size), px(mediaHeight + 36),
                           nullptr, nullptr, GetModuleHandle(nullptr), nullptr);
        int square = DWMWCP_DONOTROUND;
        COLORREF noBorder = DWMWA_COLOR_NONE;
        DwmSetWindowAttribute(app.mini, DWMWA_WINDOW_CORNER_PREFERENCE, &square, sizeof(square));
        DwmSetWindowAttribute(app.mini, DWMWA_BORDER_COLOR, &noBorder, sizeof(noBorder));
    }
    layout();
    if (old)
        DestroyWindow(old);
    invalidate();
}
LRESULT CALLBACK PreviewProc(HWND window, UINT message, WPARAM w, LPARAM l)
{
    if (message == WM_LBUTTONDBLCLK)
    {
        if (GetParent(window) == app.main || GetParent(window) == app.mini)
            openPlayer();
        else
            setFullscreen(!app.fullscreen);
        return 0;
    }
    if (message == WM_LBUTTONDOWN)
    {
        SetFocus(window);
        if (GetParent(window) == app.mini)
        {
            ReleaseCapture();
            SendMessage(app.mini, WM_NCLBUTTONDOWN, HTCAPTION, 0);
        }
        return 0;
    }
    if (message == WM_KEYDOWN)
    {
        if (w == VK_SPACE)
            toggle();
        if (w == VK_ESCAPE && app.fullscreen)
            setFullscreen(false);
        return 0;
    }
    if (message == WM_ERASEBKGND)
        return 1;
    if (message == WM_TIMER && w == 7)
    {
        if (app.features.disc && app.playback.width == 0 && app.playback.state == 3 &&
            IsWindowVisible(window))
            InvalidateRect(window, nullptr, FALSE);
        return 0;
    }
    if (message == WM_PAINT)
    {
        PAINTSTRUCT paint{};
        auto dc = BeginPaint(window, &paint);
        RECT r{};
        GetClientRect(window, &r);
        fill(dc, {0, 0, logical(r.right), logical(r.bottom)}, RGB(8, 10, 9));
        if (app.playback.width == 0)
        {
            int width = logical(r.right), height = logical(r.bottom);
            if (app.features.disc && !app.slots.empty())
            {
                int radius = std::max(10, std::min(width, height) / 2 - 6);
                circle(dc, width / 2, height / 2, radius, RGB(18, 18, 18));
                for (int ring = radius / 3; ring < radius; ring += 8)
                {
                    auto pen = CreatePen(PS_SOLID, 1, RGB(48, 48, 48));
                    auto old = SelectObject(dc, pen);
                    auto brush = SelectObject(dc, GetStockObject(HOLLOW_BRUSH));
                    Ellipse(dc, px(width / 2 - ring), px(height / 2 - ring), px(width / 2 + ring),
                            px(height / 2 + ring));
                    SelectObject(dc, old);
                    SelectObject(dc, brush);
                    DeleteObject(pen);
                }
                double elapsed = app.playback.state == 3
                                     ? double(GetTickCount64() - app.playbackReadAt) / 1000 * app.speed
                                     : 0;
                double angle = (app.playback.time + elapsed) * .8;
                line(dc, width / 2, height / 2, width / 2 + int(std::cos(angle) * radius * .7),
                     height / 2 + int(std::sin(angle) * radius * .7), colors.muted);
                circle(dc, width / 2, height / 2, std::max(3, radius / 4), colors.accent);
                circle(dc, width / 2, height / 2, 3, RGB(0, 0, 0));
            }
            else if (app.artwork)
            {
                Gdiplus::Graphics graphics(dc);
                double scale = std::min(double(r.right) / app.artwork->GetWidth(),
                                        double(r.bottom) / app.artwork->GetHeight());
                int imageWidth = int(app.artwork->GetWidth() * scale),
                    imageHeight = int(app.artwork->GetHeight() * scale);
                graphics.DrawImage(app.artwork.get(), (r.right - imageWidth) / 2,
                                   (r.bottom - imageHeight) / 2, imageWidth, imageHeight);
            }
            else
                mark(dc, {(width - 64) / 2, (height - 64) / 2, 64, 64});
        }
        EndPaint(window, &paint);
        return 0;
    }
    return DefWindowProc(window, message, w, l);
}
void openPicker()
{
    if (app.picker)
    {
        SetForegroundWindow(app.picker);
        return;
    }
    if (app.slots.size() >= 8)
        return;
    app.pickerQuery.clear();
    app.pickerScroll = 0;
    RECT r{};
    GetWindowRect(app.main, &r);
    app.picker = CreateWindowEx(WS_EX_DLGMODALFRAME, L"GreaterArtPicker", L"Greater Art · Add track",
                                WS_OVERLAPPEDWINDOW | WS_VISIBLE, r.left + px(160), r.top + px(80), px(620),
                                px(650), app.main, nullptr, GetModuleHandle(nullptr), nullptr);
    BOOL dark = TRUE;
    DwmSetWindowAttribute(app.picker, 20, &dark, sizeof(dark));
}
LRESULT CALLBACK PickerProc(HWND window, UINT message, WPARAM w, LPARAM l)
{
    if (message == WM_CREATE)
    {
        app.pickerSearch =
            CreateWindowEx(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL | WS_TABSTOP, px(20),
                           px(58), px(520), px(34), window, reinterpret_cast<HMENU>(INT_PTR(PICKER_SEARCH)),
                           GetModuleHandle(nullptr), nullptr);
        SendMessage(app.pickerSearch, WM_SETFONT, reinterpret_cast<WPARAM>(app.body), TRUE);
        SendMessage(app.pickerSearch, EM_SETCUEBANNER, FALSE, reinterpret_cast<LPARAM>(L"Filter library"));
        accessibleName(app.pickerSearch, L"Filter Library to add a track");
        return 0;
    }
    if (message == WM_COMMAND && LOWORD(w) == PICKER_SEARCH && HIWORD(w) == EN_CHANGE)
    {
        wchar_t text[1024]{};
        GetWindowText(app.pickerSearch, text, 1024);
        app.pickerQuery = text;
        app.pickerScroll = 0;
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    }
    if (message == WM_PAINT)
    {
        PAINTSTRUCT paint{};
        auto dc = BeginPaint(window, &paint);
        RECT r{};
        GetClientRect(window, &r);
        int width = logical(r.right), height = logical(r.bottom);
        fill(dc, {0, 0, width, height}, colors.panel);
        text(dc, app.slots.empty() ? L"Choose primary track" : L"Add track to Stack", {20, 8, width - 40, 38},
             app.heading, colors.text);
        fill(dc, {14, 52, width - 28, 46}, colors.raised);
        MoveWindow(app.pickerSearch, px(24), px(60), px(width - 48), px(30), TRUE);
        app.pickerHits.clear();
        auto query = lower(app.pickerQuery);
        std::vector<int> files;
        for (std::size_t i = 0; i < app.songs.size(); ++i)
            if (query.empty() || lower(app.songs[i].title).find(query) != std::wstring::npos)
                files.push_back(int(i));
        int rows = std::max(1, (height - 112) / 76);
        app.pickerScroll = std::clamp(app.pickerScroll, 0, std::max(0, int(files.size()) - rows));
        for (int i = 0; i < rows && i + app.pickerScroll < int(files.size()); ++i)
        {
            int index = files[i + app.pickerScroll];
            row(dc, app.pickerHits, {0, 108 + i * 76, width, 76}, app.songs[index], index, true);
        }
        EndPaint(window, &paint);
        return 0;
    }
    if (message == WM_LBUTTONUP)
    {
        int x = logical(GET_X_LPARAM(l)), y = logical(GET_Y_LPARAM(l));
        if (auto hit = hitAt(window, x, y))
            action(*hit, window);
        return 0;
    }
    if (message == WM_MOUSEWHEEL)
    {
        app.pickerScroll = std::max(0, app.pickerScroll - GET_WHEEL_DELTA_WPARAM(w) / WHEEL_DELTA * 3);
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    }
    if (message == WM_SIZE)
    {
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    }
    if (message == WM_DESTROY)
    {
        app.picker = nullptr;
        app.pickerSearch = nullptr;
        return 0;
    }
    if (message == WM_CTLCOLOREDIT)
    {
        SetTextColor(reinterpret_cast<HDC>(w), colors.text);
        SetBkColor(reinterpret_cast<HDC>(w), colors.raised);
        return reinterpret_cast<LRESULT>(app.editBrush);
    }
    if (message == WM_ERASEBKGND)
        return 1;
    return DefWindowProc(window, message, w, l);
}
void probeTick()
{
    if (!app.probe)
        return;
    auto fail = [&](const std::wstring &) {
        std::ofstream report(app.probeOutput / L"verification.txt", std::ios::app);
        report << "FAIL stage=" << app.probeStage << "\n";
        app.probeFailed = true;
        PostMessage(app.main, WM_CLOSE, 0, 0);
    };
    if (GetTickCount64() - app.probeAt > 45000)
    {
        fail(L"timeout");
        return;
    }
    if (app.probeStage == 0 && !app.busy && !app.slots.empty() && app.playback.state == 3 &&
        app.playback.frames > 6)
    {
        capture(app.main, app.probeOutput / L"workspace.png");
        if (!probeAccessibility(app.main))
        {
            fail(L"workspace accessible controls");
            return;
        }
        Song song = app.slots[0].song;
        for (int i = 0; i < 5; ++i)
            addSong(song);
        app.probeStage = 1;
        app.probeAt = GetTickCount64();
    }
    else if (app.probeStage == 1 && GetTickCount64() - app.probeAt > 1800)
    {
        if (app.slots.size() != 6 || std::any_of(app.slots.begin(), app.slots.end(), [](auto &slot) {
                return mediaState(slot.player).state != 3;
            }))
        {
            fail(L"voices");
            return;
        }
        capture(app.main, app.probeOutput / L"stack-six.png");
        openPlayer();
        app.probeStage = 2;
        app.probeAt = GetTickCount64();
    }
    else if (app.probeStage == 2 && GetTickCount64() - app.probeAt > 1100)
    {
        capture(app.player, app.probeOutput / L"now-playing.png");
        if (!probeAccessibility(app.player))
        {
            fail(L"player accessible controls");
            return;
        }
        setFullscreen(true);
        app.probeStage = 3;
        app.probeAt = GetTickCount64();
    }
    else if (app.probeStage == 3 && GetTickCount64() - app.probeAt > 1000)
    {
        capture(app.player, app.probeOutput / L"fullscreen.png");
        MONITORINFO m{sizeof(MONITORINFO)};
        GetMonitorInfo(MonitorFromWindow(app.player, MONITOR_DEFAULTTONEAREST), &m);
        RECT r{};
        GetWindowRect(app.player, &r);
        if (r.left != m.rcMonitor.left || r.right != m.rcMonitor.right || r.top != m.rcMonitor.top ||
            r.bottom != m.rcMonitor.bottom)
        {
            fail(L"fullscreen bounds");
            return;
        }
        setFullscreen(false);
        closePlayer();
        ShowWindow(app.main, SW_MINIMIZE);
        app.probeStage = 4;
        app.probeAt = GetTickCount64();
    }
    else if (app.probeStage == 4 && GetTickCount64() - app.probeAt > 900)
    {
        if (!app.mini || GetParent(app.preview) != app.mini || app.playback.state != 3)
        {
            fail(L"mini");
            return;
        }
        capture(app.mini, app.probeOutput / L"mini.png");
        openPlayer();
        app.probeStage = 5;
        app.probeAt = GetTickCount64();
    }
    else if (app.probeStage == 5 && GetTickCount64() - app.probeAt > 900)
    {
        if (app.playback.state != 3)
        {
            fail(L"mini expand");
            return;
        }
        closePlayer();
        ShowWindow(app.main, SW_RESTORE);
        layout();
        capture(app.main, app.probeOutput / L"returned-dock.png");
        std::ofstream report(app.probeOutput / L"verification.txt");
        report << "PASS version=0.1.3 code=4\nvoices=" << app.slots.size()
               << "\nsource=" << app.playback.width << "x" << app.playback.height
               << "\nframes=" << app.playback.frames
               << "\nfullscreen_monitor_bounds=true\nmini_and_dock_transfer=true\nmsaa_named_controls="
                  "true\nno_vlc=true\nacoustic_"
                  "alignment=NOT_VERIFIED\n";
        app.probeStage = 6;
        PostMessage(app.main, WM_CLOSE, 0, 0);
    }
}
LRESULT CALLBACK WindowProc(HWND window, UINT message, WPARAM w, LPARAM l)
{
    if (message == WM_GETOBJECT)
        return accessibilityObject(window, w, l);
    bool main = window == app.main || (!app.main && message == WM_CREATE);
    bool mini = window == app.mini;
    if (message == WM_CREATE && main)
    {
        app.main = window;
        app.search = CreateWindowEx(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | WS_TABSTOP | ES_AUTOHSCROLL, 0,
                                    0, 100, 30, window, reinterpret_cast<HMENU>(INT_PTR(SEARCH)),
                                    GetModuleHandle(nullptr), nullptr);
        app.nodeSearch = CreateWindowEx(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | WS_TABSTOP | ES_AUTOHSCROLL,
                                        0, 0, 100, 30, window, reinterpret_cast<HMENU>(INT_PTR(GRAPH_SEARCH)),
                                        GetModuleHandle(nullptr), nullptr);
        SendMessage(app.search, WM_SETFONT, reinterpret_cast<WPARAM>(app.body), TRUE);
        SendMessage(app.nodeSearch, WM_SETFONT, reinterpret_cast<WPARAM>(app.body), TRUE);
        SendMessage(app.search, EM_SETCUEBANNER, FALSE, reinterpret_cast<LPARAM>(L"Filter library"));
        SendMessage(app.nodeSearch, EM_SETCUEBANNER, FALSE, reinterpret_cast<LPARAM>(L"Find a node"));
        accessibleName(app.search, translate(L"Filter library", app.features.language).c_str());
        accessibleName(app.nodeSearch, translate(L"Find a node", app.features.language).c_str());
        app.preview = CreateWindowEx(0, L"GreaterArtPreview", L"", WS_CHILD | WS_VISIBLE | WS_CLIPSIBLINGS, 0,
                                     0, 1, 1, window, nullptr, GetModuleHandle(nullptr), nullptr);
        if (app.features.disc)
            SetTimer(app.preview, 7, 16, nullptr);
        SetTimer(window, 1, 100, nullptr);
        return 0;
    }
    if (message == WM_PAINT)
    {
        PAINTSTRUCT paint{};
        auto dc = BeginPaint(window, &paint);
        RECT r{};
        GetClientRect(window, &r);
        auto buffer = CreateCompatibleDC(dc);
        auto bitmap = CreateCompatibleBitmap(dc, std::max(1L, r.right), std::max(1L, r.bottom));
        auto old = SelectObject(buffer, bitmap);
        IntersectClipRect(buffer, paint.rcPaint.left, paint.rcPaint.top, paint.rcPaint.right,
                          paint.rcPaint.bottom);
        if (main)
            paintMain(buffer);
        else
            paintPlayer(buffer, window, mini);
        if (GetFocus() == window && app.keyboardFocus >= 0 && app.keyboardFocus < int(hitsFor(window).size()))
        {
            auto focus = physical(hitsFor(window)[app.keyboardFocus].box);
            DrawFocusRect(buffer, &focus);
        }
        BitBlt(dc, 0, 0, r.right, r.bottom, buffer, 0, 0, SRCCOPY);
        SelectObject(buffer, old);
        DeleteObject(bitmap);
        DeleteDC(buffer);
        EndPaint(window, &paint);
        return 0;
    }
    if (message == WM_ERASEBKGND)
        return 1;
    if (message == WM_SIZE)
    {
        if (main && w == SIZE_MINIMIZED && !app.slots.empty() && !app.player)
            miniPlayer();
        else if (main && w != SIZE_MINIMIZED && app.mini)
        {
            auto m = app.mini;
            app.mini = nullptr;
            positionPreview(app.main, {64, app.clientH - 104, 128, 80});
            DestroyWindow(m);
        }
        layout();
        invalidate();
        return 0;
    }
    if (message == WM_COMMAND)
    {
        if (LOWORD(w) == 401)
        {
            if (app.locked && GetForegroundWindow() == app.player)
                return 0;
            toggle();
            return 0;
        }
        if (LOWORD(w) == 402)
        {
            addFolder();
            return 0;
        }
        if (LOWORD(w) == 403)
        {
            app.settingsPage = !app.settingsPage;
            layout();
            invalidate();
            return 0;
        }
        if (HIWORD(w) == EN_CHANGE && (LOWORD(w) == SEARCH || LOWORD(w) == GRAPH_SEARCH))
        {
            wchar_t input[4096]{};
            GetWindowText(reinterpret_cast<HWND>(l), input, 4096);
            if (LOWORD(w) == SEARCH)
            {
                app.query = input;
                app.libraryScroll = 0;
                filter();
            }
            else
            {
                app.nodeQuery = input;
                invalidate();
            }
            return 0;
        }
    }
    if (message == WM_CTLCOLOREDIT)
    {
        SetTextColor(reinterpret_cast<HDC>(w), colors.text);
        SetBkColor(reinterpret_cast<HDC>(w), colors.raised);
        return reinterpret_cast<LRESULT>(app.editBrush);
    }
    if (message == SCANNED)
    {
        std::vector<App::Result> results;
        {
            std::lock_guard<std::mutex> guard(app.resultsMutex);
            results.swap(app.results);
        }
        for (auto &result : results)
            if (result.generation == app.generation)
            {
                app.songs = std::move(result.songs);
                app.graph = std::move(result.graph);
                app.graphMinX = app.graphMinY = 0;
                app.graphMaxX = app.graphMaxY = 1;
                for (auto p : app.graph.points)
                {
                    app.graphMinX = std::min(app.graphMinX, p.x);
                    app.graphMaxX = std::max(app.graphMaxX, p.x);
                    app.graphMinY = std::min(app.graphMinY, p.y);
                    app.graphMaxY = std::max(app.graphMaxY, p.y);
                }
                app.busy = false;
                filter();
                if (!app.probe && app.slots.empty() && app.features.resume && !app.features.lastPath.empty())
                {
                    auto previous = std::find_if(app.songs.begin(), app.songs.end(), [](auto &song) {
                        return song.path == app.features.lastPath;
                    });
                    if (previous != app.songs.end())
                    {
                        double position = app.features.lastPosition;
                        playSong(*previous);
                        mediaSeek(app.slots[0].player, position);
                        mediaPause(app.slots[0].player, true);
                    }
                }
            }
        return 0;
    }
    if (message == ALIGN_READY)
    {
        alignmentReady();
        return 0;
    }
    if (message == ASSET_READY)
    {
        assetReady();
        return 0;
    }
    if (message == SHARE_READY)
    {
        showWindowsShare(app.player ? app.player : app.main);
        return 0;
    }
    if (message == SYSTEM_MEDIA)
    {
        auto button = winrt::Windows::Media::SystemMediaTransportControlsButton(w);
        if (button == winrt::Windows::Media::SystemMediaTransportControlsButton::Play)
        {
            if (app.playback.state != 3)
                toggle();
        }
        else if (button == winrt::Windows::Media::SystemMediaTransportControlsButton::Pause)
        {
            if (app.playback.state == 3)
                toggle();
        }
        else if (button == winrt::Windows::Media::SystemMediaTransportControlsButton::Next)
            next(1);
        else if (button == winrt::Windows::Media::SystemMediaTransportControlsButton::Previous)
            next(-1);
        return 0;
    }
    if (message == WM_TIMER && main && w == 4)
    {
        if (GetTickCount64() - app.tapAt >= 180)
        {
            app.tapPath.clear();
            KillTimer(window, 4);
        }
        auto region = physical(app.libraryRect);
        InvalidateRect(window, &region, FALSE);
        if (app.player)
            InvalidateRect(app.player, nullptr, FALSE);
        return 0;
    }
    if (message == WM_TIMER && main && w == 5)
    {
        if (app.slots.empty())
        {
            KillTimer(window, 5);
            return 0;
        }
        if (!IsIconic(window))
        {
            auto region = physical({48, app.clientH - dockHeight(), app.clientW - 48, dockHeight()});
            InvalidateRect(window, &region, FALSE);
        }
        if (app.player && !app.fullscreen)
        {
            auto region = physical(
                {0, app.previewRect.y + app.previewRect.h, logical(GetSystemMetrics(SM_CXSCREEN)), 80});
            InvalidateRect(app.player, &region, FALSE);
        }
        return 0;
    }
    if (message == WM_TIMER && main)
    {
        app.now = GetTickCount64();
        app.assetPermit = app.slots.size() < 2 && !app.aligning &&
                          (app.playback.state == 4 ||
                           (app.playback.state == 3 && (app.playback.width == 0 || app.playback.frames > 2)));
        if (!app.slots.empty())
        {
            auto oldWidth = app.playback.width, oldHeight = app.playback.height;
            app.playback = mediaState(app.slots[0].player);
            app.playbackReadAt = app.now;
            if (oldWidth != app.playback.width || oldHeight != app.playback.height)
                layout();
            if (app.playback.state == 7 && app.previousState != 7)
                toast(L"Windows cannot decode this file. Its codec may be unavailable.");
            if (app.playback.state == 6 && app.previousState != 6)
            {
                if (app.settings.loop && (app.features.repeat == 1 || app.slots.size() > 1))
                {
                    seek(0);
                    if (app.slots.size() > 1)
                    {
                        app.barrier = true;
                        app.barrierAt = app.now;
                    }
                    for (auto &slot : app.slots)
                        if (!app.barrier)
                            mediaPlay(slot.player);
                }
                else
                {
                    bool last =
                        app.filtered.empty() || app.slots[0].song.path == app.songs[app.filtered.back()].path;
                    if (app.features.repeat != 0 || !last)
                        next(1);
                }
            }
            app.previousState = app.playback.state;
            if (app.practice.shouldLoop(app.playback.time))
                seek(app.practice.a);
            if (app.sleepUntil && app.now >= app.sleepUntil)
            {
                for (auto &slot : app.slots)
                    mediaPause(slot.player, true);
                app.sleepUntil = 0;
                toast(L"Sleep timer finished.");
            }
            syncStack();
            if (app.lyricsPanel && app.details)
            {
                static int old = -2;
                int index = activeLyric(app.lyrics, app.playback.time);
                if (index != old && index >= 0)
                {
                    old = index;
                    int line = int(index);
                    SendMessage(GetDlgItem(app.details, 501), EM_LINESCROLL, 0,
                                line -
                                    SendMessage(GetDlgItem(app.details, 501), EM_GETFIRSTVISIBLELINE, 0, 0));
                }
            }
            static ULONGLONG lastResumeSave = 0;
            if (app.features.resume && app.now - lastResumeSave >= 5000)
            {
                lastResumeSave = app.now;
                app.features.lastPosition = app.playback.time;
                saveFeatures(app.dataDir, app.features);
            }
        }
        if (app.graphReveal >= 0)
        {
            app.graphReveal += std::max(1, int(app.graph.points.size() / 25));
            if (app.graphReveal >= int(app.graph.points.size()))
                app.graphReveal = -1;
        }
        invalidate();
        probeTick();
        publishSystemMedia();
        if (app.playback.width == 0 && app.preview)
            InvalidateRect(app.preview, nullptr, FALSE);
        return 0;
    }
    if (message == WM_LBUTTONDOWN)
    {
        SetFocus(window);
        int x = logical(GET_X_LPARAM(l)), y = logical(GET_Y_LPARAM(l));
        if (app.features.developer && (GetKeyState(VK_CONTROL) & 0x8000))
            return 0;
        if (auto hit = hitAt(window, x, y))
        {
            if (hit->action == SEEK || hit->action == GAIN_SLOT || hit->action == MASTER_GAIN)
            {
                if (app.locked && window == app.player)
                    return 0;
                app.drag = hit->action;
                app.dragSlot = hit->data;
                SetCapture(window);
                applyDrag(window, x, y);
                return 0;
            }
            if (hit->action == NONE && hit->data)
            {
                app.drag = NONE;
                app.dragSlot = hit->data + 100;
                app.dragStartX = x;
                app.startLeft = app.settings.leftWidth;
                app.startRight = app.settings.rightWidth;
                SetCapture(window);
                return 0;
            }
        }
        if (main && app.nodesRect.contains(x, y) && y > app.nodesRect.y + 102 &&
            y < app.nodesRect.y + app.nodesRect.h - 40)
        {
            app.dragSlot = 200;
            app.dragStartX = x;
            app.dragStartY = y;
            app.graphDrag = -1;
            app.graphMoved = false;
            for (std::size_t i = 0; i < app.graph.points.size(); ++i)
            {
                auto p = graphPoint(i);
                if (std::hypot(p.x - x, p.y - y) < 18)
                {
                    app.graphDrag = int(i);
                    break;
                }
            }
            SetCapture(window);
            return 0;
        }
        return 0;
    }
    if (message == WM_MOUSEMOVE && GetCapture() == window)
    {
        int x = logical(GET_X_LPARAM(l)), y = logical(GET_Y_LPARAM(l));
        if (app.dragSlot == 101)
        {
            app.settings.leftWidth = app.startLeft + x - app.dragStartX;
            layout();
            invalidate();
        }
        else if (app.dragSlot == 102)
        {
            app.settings.rightWidth = app.startRight - x + app.dragStartX;
            layout();
            invalidate();
        }
        else if (app.dragSlot == 200)
        {
            double dx = x - app.dragStartX, dy = y - app.dragStartY;
            if (std::abs(dx) + std::abs(dy) > 2)
                app.graphMoved = true;
            if (app.graphDrag >= 0)
            {
                auto r = app.nodesRect;
                double scale = std::min((r.w - 52) / std::max(1.0, app.graphMaxX - app.graphMinX),
                                        (r.h - 180) / std::max(1.0, app.graphMaxY - app.graphMinY)) *
                               app.zoom;
                if (scale > 0)
                {
                    app.graph.points[app.graphDrag].x += dx / scale;
                    app.graph.points[app.graphDrag].y += dy / scale;
                }
            }
            else
            {
                app.panX += dx;
                app.panY += dy;
            }
            app.dragStartX = x;
            app.dragStartY = y;
            invalidate();
        }
        else
            applyDrag(window, x, y);
        return 0;
    }
    if (message == WM_LBUTTONUP)
    {
        int x = logical(GET_X_LPARAM(l)), y = logical(GET_Y_LPARAM(l));
        if (app.features.developer && (GetKeyState(VK_CONTROL) & 0x8000))
        {
            if (auto hit = hitAt(window, x, y))
                inspectElement(window, *hit);
            return 0;
        }
        if (GetCapture() == window)
        {
            ReleaseCapture();
            if (app.dragSlot == 101 || app.dragSlot == 102)
                persist();
            else if (app.dragSlot == 200 && app.graphDrag >= 0 && !app.graphMoved &&
                     std::hypot(double(x - app.dragStartX), double(y - app.dragStartY)) < 3 &&
                     app.graphDrag < int(app.songs.size()))
                playSong(app.songs[app.graphDrag]);
            app.drag = NONE;
            app.dragSlot = 0;
            return 0;
        }
        if (auto hit = hitAt(window, x, y))
            action(*hit, window);
        return 0;
    }
    if (message == WM_RBUTTONUP)
    {
        int x = logical(GET_X_LPARAM(l)), y = logical(GET_Y_LPARAM(l));
        if (auto hit = hitAt(window, x, y))
            if (hit->action == PLAY_SONG)
                sourceMenu(hit->data, window);
        return 0;
    }
    if (message == WM_MOUSEWHEEL)
    {
        POINT cursor{GET_X_LPARAM(l), GET_Y_LPARAM(l)};
        ScreenToClient(window, &cursor);
        int x = logical(cursor.x), y = logical(cursor.y), delta = GET_WHEEL_DELTA_WPARAM(w) / WHEEL_DELTA;
        if (main && app.nodesRect.contains(x, y))
        {
            app.zoom = std::clamp(app.zoom * (delta > 0 ? 1.15 : .87), .35, 5.0);
        }
        else if (main && app.stackRect.contains(x, y))
            app.stackScroll = std::clamp(app.stackScroll - delta * 48, 0,
                                         std::max(0, int(app.slots.size()) * 184 - app.stackRect.h + 100));
        else if (main && app.settingsPage)
            app.settingsScroll = std::clamp(app.settingsScroll - delta * 48, 0, 1800);
        else
            app.libraryScroll = std::max(0, app.libraryScroll - delta * 3);
        invalidate();
        return 0;
    }
    if (message == WM_KEYDOWN)
    {
        auto &controls = hitsFor(window);
        if (w == VK_TAB && !controls.empty())
        {
            app.keyboardFocus =
                (app.keyboardFocus + ((GetKeyState(VK_SHIFT) & 0x8000) ? -1 : 1) + int(controls.size())) %
                int(controls.size());
            NotifyWinEvent(EVENT_OBJECT_FOCUS, window, OBJID_CLIENT,
                           accessibleChildId(window, app.keyboardFocus));
            invalidate();
            return 0;
        }
        if ((w == VK_LEFT || w == VK_RIGHT) && app.keyboardFocus >= 0 &&
            app.keyboardFocus < int(controls.size()))
        {
            auto &hit = controls[app.keyboardFocus];
            if (app.locked && window == app.player)
                return 0;
            int step = w == VK_RIGHT ? 1 : -1;
            if (hit.action == SEEK)
                seek(app.playback.time + step * 5);
            else if (hit.action == MASTER_GAIN)
            {
                app.master = std::clamp(app.master + step * 5, 0, 100);
                gains();
            }
            else if (hit.action == GAIN_SLOT && hit.data >= 0 && hit.data < int(app.slots.size()))
            {
                app.slots[hit.data].gain = std::clamp(app.slots[hit.data].gain + step * 5, 0, 100);
                gains();
            }
            invalidate();
            return 0;
        }
        if ((w == VK_RETURN || w == VK_SPACE) && app.keyboardFocus >= 0 &&
            app.keyboardFocus < int(controls.size()))
        {
            action(controls[app.keyboardFocus], window);
            return 0;
        }
        if (w == VK_F11)
        {
            if (!app.player)
                openPlayer();
            setFullscreen(!app.fullscreen);
        }
        if (w == VK_ESCAPE && app.fullscreen)
            setFullscreen(false);
        return 0;
    }
    if (message == WM_NCHITTEST && mini)
    {
        auto result = DefWindowProc(window, message, w, l);
        if (result == HTCLIENT)
        {
            POINT point{GET_X_LPARAM(l), GET_Y_LPARAM(l)};
            ScreenToClient(window, &point);
            if (logical(point.y) < app.previewRect.h)
                return HTCAPTION;
        }
        return result;
    }
    if (message == WM_DPICHANGED && main)
    {
        app.dpi = HIWORD(w);
        fonts();
        theme();
        auto r = reinterpret_cast<RECT *>(l);
        SetWindowPos(window, nullptr, r->left, r->top, r->right - r->left, r->bottom - r->top,
                     SWP_NOZORDER | SWP_NOACTIVATE);
        SendMessage(app.search, WM_SETFONT, reinterpret_cast<WPARAM>(app.body), TRUE);
        SendMessage(app.nodeSearch, WM_SETFONT, reinterpret_cast<WPARAM>(app.body), TRUE);
        layout();
        return 0;
    }
    if (message == WM_GETMINMAXINFO)
    {
        auto info = reinterpret_cast<MINMAXINFO *>(l);
        MONITORINFO monitor{sizeof(MONITORINFO)};
        GetMonitorInfo(MonitorFromWindow(window, MONITOR_DEFAULTTONEAREST), &monitor);
        info->ptMinTrackSize = {
            std::min(px(main ? 800 : 480), int(monitor.rcWork.right - monitor.rcWork.left)),
            std::min(px(main ? 540 : 460), int(monitor.rcWork.bottom - monitor.rcWork.top))};
        return 0;
    }
    if (message == WM_CLOSE)
    {
        if (main)
        {
            if (!app.probe && !app.exiting && !app.slots.empty())
            {
                ShowWindow(window, SW_MINIMIZE);
                return 0;
            }
            app.closing = true;
            ++app.generation;
            KillTimer(window, 1);
            if (!app.slots.empty())
            {
                app.features.lastPosition = app.playback.time;
                persist();
            }
            app.scans.shutdown();
            releaseSlots();
            mediaShutdown();
            if (app.player)
            {
                auto p = app.player;
                app.player = nullptr;
                DestroyWindow(p);
            }
            if (app.mini)
            {
                auto m = app.mini;
                app.mini = nullptr;
                DestroyWindow(m);
            }
            DestroyWindow(window);
        }
        else if (mini)
        {
            auto m = app.mini;
            app.mini = nullptr;
            positionPreview(app.main, {64, app.clientH - 104, 128, 80});
            DestroyWindow(m);
            releaseSlots();
        }
        else
            closePlayer();
        return 0;
    }
    if (message == WM_DESTROY && main)
    {
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProc(window, message, w, l);
}
} // namespace
