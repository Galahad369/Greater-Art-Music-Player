namespace {
void capture(HWND window, const std::filesystem::path &file) {
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
    if (!PrintWindow(window, memory, 2)) {
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
void setFullscreen(bool value) {
    if (!app.player)
        return;
    if (value == app.fullscreen)
        return;
    app.fullscreen = value;
    if (value) {
        GetWindowPlacement(app.player, &app.placement);
        SetWindowLongPtr(app.player, GWL_STYLE, WS_POPUP | WS_VISIBLE | WS_CLIPCHILDREN);
        MONITORINFO monitor{sizeof(MONITORINFO)};
        GetMonitorInfo(MonitorFromWindow(app.player, MONITOR_DEFAULTTONEAREST), &monitor);
        SetWindowPos(app.player, HWND_TOP, monitor.rcMonitor.left, monitor.rcMonitor.top,
                     monitor.rcMonitor.right - monitor.rcMonitor.left,
                     monitor.rcMonitor.bottom - monitor.rcMonitor.top, SWP_FRAMECHANGED);
    } else {
        SetWindowLongPtr(app.player, GWL_STYLE, WS_OVERLAPPEDWINDOW | WS_VISIBLE | WS_CLIPCHILDREN);
        SetWindowPlacement(app.player, &app.placement);
        SetWindowPos(app.player, nullptr, 0, 0, 0, 0,
                     SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER);
    }
    layout();
    invalidate();
}
void closePlayer() {
    if (!app.player)
        return;
    auto window = app.player;
    app.fullscreen = false;
    app.player = nullptr;
    positionPreview(app.main, {64, app.clientH - 104, 128, 80});
    DestroyWindow(window);
    if (IsIconic(app.main))
        miniPlayer();
    else {
        layout();
        invalidate();
    }
}
void openPlayer() {
    if (app.slots.empty())
        return;
    if (app.player) {
        ShowWindow(app.player, SW_RESTORE);
        SetForegroundWindow(app.player);
        return;
    }
    if (app.mini) {
        auto mini = app.mini;
        app.mini = nullptr;
        positionPreview(app.main, {64, app.clientH - 104, 128, 80});
        DestroyWindow(mini);
    }
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
    SetForegroundWindow(app.player);
}
void miniPlayer() {
    if (app.slots.empty())
        return;
    auto old = app.player;
    app.player = nullptr;
    app.fullscreen = false;
    if (!app.mini) {
        MONITORINFO monitor{sizeof(MONITORINFO)};
        GetMonitorInfo(MonitorFromWindow(app.main, MONITOR_DEFAULTTONEAREST), &monitor);
        int size = 192 + app.settings.miniSize * 48;
        app.mini =
            CreateWindowEx(WS_EX_TOPMOST | WS_EX_TOOLWINDOW, L"GreaterArtMini", L"Greater Art Mini",
                           WS_POPUP | WS_VISIBLE | WS_CLIPCHILDREN, monitor.rcWork.right - px(size) - 16,
                           monitor.rcWork.bottom - px(size * 9 / 16 + 36) - 16, px(size),
                           px(size * 9 / 16 + 36), nullptr, nullptr, GetModuleHandle(nullptr), nullptr);
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
LRESULT CALLBACK PreviewProc(HWND window, UINT message, WPARAM w, LPARAM l) {
    if (message == WM_LBUTTONDBLCLK) {
        if (GetParent(window) == app.main || GetParent(window) == app.mini)
            openPlayer();
        else
            setFullscreen(!app.fullscreen);
        return 0;
    }
    if (message == WM_LBUTTONDOWN) {
        SetFocus(window);
        if (GetParent(window) == app.mini) {
            ReleaseCapture();
            SendMessage(app.mini, WM_NCLBUTTONDOWN, HTCAPTION, 0);
        }
        return 0;
    }
    if (message == WM_KEYDOWN) {
        if (w == VK_SPACE)
            toggle();
        if (w == VK_ESCAPE && app.fullscreen)
            setFullscreen(false);
        return 0;
    }
    if (message == WM_ERASEBKGND)
        return 1;
    if (message == WM_PAINT) {
        PAINTSTRUCT paint{};
        auto dc = BeginPaint(window, &paint);
        RECT r{};
        GetClientRect(window, &r);
        fill(dc, {0, 0, logical(r.right), logical(r.bottom)}, RGB(8, 10, 9));
        if (app.playback.width == 0)
            mark(dc, {(logical(r.right) - 64) / 2, (logical(r.bottom) - 64) / 2, 64, 64});
        EndPaint(window, &paint);
        return 0;
    }
    return DefWindowProc(window, message, w, l);
}
void openPicker() {
    if (app.picker) {
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
LRESULT CALLBACK PickerProc(HWND window, UINT message, WPARAM w, LPARAM l) {
    if (message == WM_CREATE) {
        app.pickerSearch =
            CreateWindowEx(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL | WS_TABSTOP, px(20),
                           px(58), px(520), px(34), window, reinterpret_cast<HMENU>(INT_PTR(PICKER_SEARCH)),
                           GetModuleHandle(nullptr), nullptr);
        SendMessage(app.pickerSearch, WM_SETFONT, reinterpret_cast<WPARAM>(app.body), TRUE);
        SendMessage(app.pickerSearch, EM_SETCUEBANNER, FALSE, reinterpret_cast<LPARAM>(L"Filter library"));
        return 0;
    }
    if (message == WM_COMMAND && LOWORD(w) == PICKER_SEARCH && HIWORD(w) == EN_CHANGE) {
        wchar_t text[1024]{};
        GetWindowText(app.pickerSearch, text, 1024);
        app.pickerQuery = text;
        app.pickerScroll = 0;
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    }
    if (message == WM_PAINT) {
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
        for (int i = 0; i < rows && i + app.pickerScroll < int(files.size()); ++i) {
            int index = files[i + app.pickerScroll];
            row(dc, app.pickerHits, {0, 108 + i * 76, width, 76}, app.songs[index], index, true);
        }
        EndPaint(window, &paint);
        return 0;
    }
    if (message == WM_LBUTTONUP) {
        int x = logical(GET_X_LPARAM(l)), y = logical(GET_Y_LPARAM(l));
        if (auto hit = hitAt(window, x, y))
            action(*hit, window);
        return 0;
    }
    if (message == WM_MOUSEWHEEL) {
        app.pickerScroll = std::max(0, app.pickerScroll - GET_WHEEL_DELTA_WPARAM(w) / WHEEL_DELTA * 3);
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    }
    if (message == WM_SIZE) {
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    }
    if (message == WM_DESTROY) {
        app.picker = nullptr;
        app.pickerSearch = nullptr;
        return 0;
    }
    if (message == WM_CTLCOLOREDIT) {
        SetTextColor(reinterpret_cast<HDC>(w), colors.text);
        SetBkColor(reinterpret_cast<HDC>(w), colors.raised);
        return reinterpret_cast<LRESULT>(app.editBrush);
    }
    if (message == WM_ERASEBKGND)
        return 1;
    return DefWindowProc(window, message, w, l);
}
void probeTick() {
    if (!app.probe)
        return;
    auto fail = [&](const std::wstring &) {
        std::ofstream report(app.probeOutput / L"verification.txt", std::ios::app);
        report << "FAIL stage=" << app.probeStage << "\n";
        app.probeFailed = true;
        PostMessage(app.main, WM_CLOSE, 0, 0);
    };
    if (GetTickCount64() - app.probeAt > 45000) {
        fail(L"timeout");
        return;
    }
    if (app.probeStage == 0 && !app.busy && !app.slots.empty() && app.playback.state == 3 &&
        app.playback.frames > 6) {
        capture(app.main, app.probeOutput / L"workspace.png");
        Song song = app.slots[0].song;
        for (int i = 0; i < 5; ++i)
            addSong(song);
        app.probeStage = 1;
        app.probeAt = GetTickCount64();
    } else if (app.probeStage == 1 && GetTickCount64() - app.probeAt > 1800) {
        if (app.slots.size() != 6 || std::any_of(app.slots.begin(), app.slots.end(), [](auto &slot) {
                return mediaState(slot.player).state != 3;
            })) {
            fail(L"voices");
            return;
        }
        capture(app.main, app.probeOutput / L"stack-six.png");
        openPlayer();
        app.probeStage = 2;
        app.probeAt = GetTickCount64();
    } else if (app.probeStage == 2 && GetTickCount64() - app.probeAt > 1100) {
        capture(app.player, app.probeOutput / L"now-playing.png");
        setFullscreen(true);
        app.probeStage = 3;
        app.probeAt = GetTickCount64();
    } else if (app.probeStage == 3 && GetTickCount64() - app.probeAt > 1000) {
        capture(app.player, app.probeOutput / L"fullscreen.png");
        MONITORINFO m{sizeof(MONITORINFO)};
        GetMonitorInfo(MonitorFromWindow(app.player, MONITOR_DEFAULTTONEAREST), &m);
        RECT r{};
        GetWindowRect(app.player, &r);
        if (r.left != m.rcMonitor.left || r.right != m.rcMonitor.right || r.top != m.rcMonitor.top ||
            r.bottom != m.rcMonitor.bottom) {
            fail(L"fullscreen bounds");
            return;
        }
        setFullscreen(false);
        closePlayer();
        ShowWindow(app.main, SW_MINIMIZE);
        app.probeStage = 4;
        app.probeAt = GetTickCount64();
    } else if (app.probeStage == 4 && GetTickCount64() - app.probeAt > 900) {
        if (!app.mini || GetParent(app.preview) != app.mini || app.playback.state != 3) {
            fail(L"mini");
            return;
        }
        capture(app.mini, app.probeOutput / L"mini.png");
        openPlayer();
        app.probeStage = 5;
        app.probeAt = GetTickCount64();
    } else if (app.probeStage == 5 && GetTickCount64() - app.probeAt > 900) {
        if (app.playback.state != 3) {
            fail(L"mini expand");
            return;
        }
        closePlayer();
        ShowWindow(app.main, SW_RESTORE);
        layout();
        capture(app.main, app.probeOutput / L"returned-dock.png");
        std::ofstream report(app.probeOutput / L"verification.txt");
        report << "PASS version=0.1.1 code=2\nvoices=" << app.slots.size()
               << "\nsource=" << app.playback.width << "x" << app.playback.height
               << "\nframes=" << app.playback.frames
               << "\nfullscreen_monitor_bounds=true\nmini_and_dock_transfer=true\nno_vlc=true\nacoustic_"
                  "alignment=NOT_VERIFIED\n";
        app.probeStage = 6;
        PostMessage(app.main, WM_CLOSE, 0, 0);
    }
}
LRESULT CALLBACK WindowProc(HWND window, UINT message, WPARAM w, LPARAM l) {
    bool main = window == app.main || (!app.main && message == WM_CREATE);
    bool mini = window == app.mini;
    if (message == WM_CREATE && main) {
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
        app.preview = CreateWindowEx(0, L"GreaterArtPreview", L"", WS_CHILD | WS_VISIBLE | WS_CLIPSIBLINGS, 0,
                                     0, 1, 1, window, nullptr, GetModuleHandle(nullptr), nullptr);
        SetTimer(window, 1, 100, nullptr);
        return 0;
    }
    if (message == WM_PAINT) {
        PAINTSTRUCT paint{};
        auto dc = BeginPaint(window, &paint);
        RECT r{};
        GetClientRect(window, &r);
        auto buffer = CreateCompatibleDC(dc);
        auto bitmap = CreateCompatibleBitmap(dc, std::max(1L, r.right), std::max(1L, r.bottom));
        auto old = SelectObject(buffer, bitmap);
        if (main)
            paintMain(buffer);
        else
            paintPlayer(buffer, window, mini);
        BitBlt(dc, 0, 0, r.right, r.bottom, buffer, 0, 0, SRCCOPY);
        SelectObject(buffer, old);
        DeleteObject(bitmap);
        DeleteDC(buffer);
        EndPaint(window, &paint);
        return 0;
    }
    if (message == WM_ERASEBKGND)
        return 1;
    if (message == WM_SIZE) {
        if (main && w == SIZE_MINIMIZED && !app.slots.empty())
            miniPlayer();
        else if (main && w != SIZE_MINIMIZED && app.mini) {
            auto m = app.mini;
            app.mini = nullptr;
            positionPreview(app.main, {64, app.clientH - 104, 128, 80});
            DestroyWindow(m);
        }
        layout();
        invalidate();
        return 0;
    }
    if (message == WM_COMMAND) {
        if (LOWORD(w) == 401) {
            toggle();
            return 0;
        }
        if (LOWORD(w) == 402) {
            addFolder();
            return 0;
        }
        if (LOWORD(w) == 403) {
            app.settingsPage = !app.settingsPage;
            layout();
            invalidate();
            return 0;
        }
        if (HIWORD(w) == EN_CHANGE && (LOWORD(w) == SEARCH || LOWORD(w) == GRAPH_SEARCH)) {
            wchar_t input[4096]{};
            GetWindowText(reinterpret_cast<HWND>(l), input, 4096);
            if (LOWORD(w) == SEARCH) {
                app.query = input;
                app.libraryScroll = 0;
                filter();
            } else {
                app.nodeQuery = input;
                invalidate();
            }
            return 0;
        }
    }
    if (message == WM_CTLCOLOREDIT) {
        SetTextColor(reinterpret_cast<HDC>(w), colors.text);
        SetBkColor(reinterpret_cast<HDC>(w), colors.raised);
        return reinterpret_cast<LRESULT>(app.editBrush);
    }
    if (message == SCANNED) {
        std::vector<App::Result> results;
        {
            std::lock_guard<std::mutex> guard(app.resultsMutex);
            results.swap(app.results);
        }
        for (auto &result : results)
            if (result.generation == app.generation) {
                app.songs = std::move(result.songs);
                app.graph = std::move(result.graph);
                app.graphMinX = app.graphMinY = 0;
                app.graphMaxX = app.graphMaxY = 1;
                for (auto p : app.graph.points) {
                    app.graphMinX = std::min(app.graphMinX, p.x);
                    app.graphMaxX = std::max(app.graphMaxX, p.x);
                    app.graphMinY = std::min(app.graphMinY, p.y);
                    app.graphMaxY = std::max(app.graphMaxY, p.y);
                }
                app.busy = false;
                filter();
            }
        return 0;
    }
    if (message == WM_TIMER && main) {
        app.now = GetTickCount64();
        if (!app.slots.empty()) {
            app.playback = mediaState(app.slots[0].player);
            if (app.playback.state == 7 && app.previousState != 7)
                toast(L"Windows cannot decode this file. Its codec may be unavailable.");
            if (app.playback.state == 6 && app.previousState != 6) {
                if (app.settings.loop) {
                    seek(0);
                    for (auto &slot : app.slots)
                        mediaPlay(slot.player);
                } else
                    next(1);
            }
            app.previousState = app.playback.state;
        }
        if (app.graphReveal >= 0) {
            app.graphReveal += std::max(1, int(app.graph.points.size() / 25));
            if (app.graphReveal >= int(app.graph.points.size()))
                app.graphReveal = -1;
        }
        invalidate();
        probeTick();
        return 0;
    }
    if (message == WM_LBUTTONDOWN) {
        int x = logical(GET_X_LPARAM(l)), y = logical(GET_Y_LPARAM(l));
        if (auto hit = hitAt(window, x, y)) {
            if (hit->action == SEEK || hit->action == GAIN_SLOT || hit->action == MASTER_GAIN) {
                app.drag = hit->action;
                app.dragSlot = hit->data;
                SetCapture(window);
                applyDrag(window, x, y);
                return 0;
            }
            if (hit->action == NONE && hit->data) {
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
            y < app.nodesRect.y + app.nodesRect.h - 40) {
            app.dragSlot = 200;
            app.dragStartX = x;
            app.dragStartY = y;
            app.graphDrag = -1;
            app.graphMoved = false;
            for (std::size_t i = 0; i < app.graph.points.size(); ++i) {
                auto p = graphPoint(i);
                if (std::hypot(p.x - x, p.y - y) < 18) {
                    app.graphDrag = int(i);
                    break;
                }
            }
            SetCapture(window);
            return 0;
        }
        return 0;
    }
    if (message == WM_MOUSEMOVE && GetCapture() == window) {
        int x = logical(GET_X_LPARAM(l)), y = logical(GET_Y_LPARAM(l));
        if (app.dragSlot == 101) {
            app.settings.leftWidth = app.startLeft + x - app.dragStartX;
            layout();
            invalidate();
        } else if (app.dragSlot == 102) {
            app.settings.rightWidth = app.startRight - x + app.dragStartX;
            layout();
            invalidate();
        } else if (app.dragSlot == 200) {
            double dx = x - app.dragStartX, dy = y - app.dragStartY;
            if (std::abs(dx) + std::abs(dy) > 2)
                app.graphMoved = true;
            if (app.graphDrag >= 0) {
                auto r = app.nodesRect;
                double scale = std::min((r.w - 52) / std::max(1.0, app.graphMaxX - app.graphMinX),
                                        (r.h - 180) / std::max(1.0, app.graphMaxY - app.graphMinY)) *
                               app.zoom;
                if (scale > 0) {
                    app.graph.points[app.graphDrag].x += dx / scale;
                    app.graph.points[app.graphDrag].y += dy / scale;
                }
            } else {
                app.panX += dx;
                app.panY += dy;
            }
            app.dragStartX = x;
            app.dragStartY = y;
            invalidate();
        } else
            applyDrag(window, x, y);
        return 0;
    }
    if (message == WM_LBUTTONUP) {
        int x = logical(GET_X_LPARAM(l)), y = logical(GET_Y_LPARAM(l));
        if (GetCapture() == window) {
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
    if (message == WM_RBUTTONUP) {
        int x = logical(GET_X_LPARAM(l)), y = logical(GET_Y_LPARAM(l));
        if (auto hit = hitAt(window, x, y))
            if (hit->action == PLAY_SONG)
                sourceMenu(hit->data, window);
        return 0;
    }
    if (message == WM_MOUSEWHEEL) {
        POINT cursor{GET_X_LPARAM(l), GET_Y_LPARAM(l)};
        ScreenToClient(window, &cursor);
        int x = logical(cursor.x), y = logical(cursor.y), delta = GET_WHEEL_DELTA_WPARAM(w) / WHEEL_DELTA;
        if (main && app.nodesRect.contains(x, y)) {
            app.zoom = std::clamp(app.zoom * (delta > 0 ? 1.15 : .87), .35, 5.0);
        } else if (main && app.stackRect.contains(x, y))
            app.stackScroll = std::clamp(app.stackScroll - delta * 48, 0,
                                         std::max(0, int(app.slots.size()) * 144 - app.stackRect.h + 100));
        else if (main && app.settingsPage)
            app.settingsScroll = std::clamp(app.settingsScroll - delta * 48, 0, 1800);
        else
            app.libraryScroll = std::max(0, app.libraryScroll - delta * 3);
        invalidate();
        return 0;
    }
    if (message == WM_KEYDOWN) {
        if (w == VK_F11) {
            if (!app.player)
                openPlayer();
            setFullscreen(!app.fullscreen);
        }
        if (w == VK_ESCAPE && app.fullscreen)
            setFullscreen(false);
        return 0;
    }
    if (message == WM_NCHITTEST && mini) {
        auto result = DefWindowProc(window, message, w, l);
        if (result == HTCLIENT) {
            POINT point{GET_X_LPARAM(l), GET_Y_LPARAM(l)};
            ScreenToClient(window, &point);
            if (logical(point.y) < app.previewRect.h)
                return HTCAPTION;
        }
        return result;
    }
    if (message == WM_DPICHANGED && main) {
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
    if (message == WM_GETMINMAXINFO) {
        auto info = reinterpret_cast<MINMAXINFO *>(l);
        MONITORINFO monitor{sizeof(MONITORINFO)};
        GetMonitorInfo(MonitorFromWindow(window, MONITOR_DEFAULTTONEAREST), &monitor);
        info->ptMinTrackSize = {
            std::min(px(main ? 800 : 480), int(monitor.rcWork.right - monitor.rcWork.left)),
            std::min(px(main ? 540 : 460), int(monitor.rcWork.bottom - monitor.rcWork.top))};
        return 0;
    }
    if (message == WM_CLOSE) {
        if (main) {
            app.closing = true;
            ++app.generation;
            KillTimer(window, 1);
            releaseSlots();
            mediaShutdown();
            if (app.player) {
                auto p = app.player;
                app.player = nullptr;
                DestroyWindow(p);
            }
            if (app.mini) {
                auto m = app.mini;
                app.mini = nullptr;
                DestroyWindow(m);
            }
            DestroyWindow(window);
        } else if (mini) {
            auto m = app.mini;
            app.mini = nullptr;
            positionPreview(app.main, {64, app.clientH - 104, 128, 80});
            DestroyWindow(m);
            releaseSlots();
        } else
            closePlayer();
        return 0;
    }
    if (message == WM_DESTROY && main) {
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProc(window, message, w, l);
}
} // namespace

