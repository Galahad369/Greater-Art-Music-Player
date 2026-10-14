namespace
{
std::wstring actionName(const Hit &hit)
{
    switch (hit.action)
    {
    case PLAY_SONG:
    case ADD_SONG:
        if (hit.data >= 0 && hit.data < int(app.songs.size()))
            return (hit.action == ADD_SONG ? L"Add to Stack: " : L"Play: ") + app.songs[hit.data].title;
        return L"Track";
    case PLAY_PAUSE:
        return app.playback.state == 3 ? L"Pause" : L"Play";
    case SEEK:
        return L"Playback timeline";
    case PREVIOUS:
        return L"Previous";
    case NEXT:
        return L"Next";
    case REPEAT:
        return L"Repeat";
    case SPEED:
        return L"Playback speed";
    case TOOLS:
        return L"Player tools";
    case TRANSPORT_LOCK:
        return app.locked ? L"Unlock playback controls" : L"Lock playback controls";
    case ADD_FOLDER:
        return L"Add folder";
    case REFRESH:
        return L"Refresh Library";
    case OPEN_SETTINGS:
    case SETTINGS_TAB:
        return L"Settings";
    case LIBRARY_TAB:
        return L"Library";
    case TOGGLE_STACK:
        return L"Toggle Stack pane";
    case TOGGLE_NODES:
        return L"Toggle Nodes pane";
    case ALL_FILES:
        return L"All songs";
    case FAVORITES:
        return L"Favorites";
    case FAVORITE_SONG:
        return L"Toggle favorite";
    case CLEAR_SEARCH:
        return L"Clear search";
    case SORT:
        return L"Sort Library";
    case ADD_STACK:
        return L"Add track to Stack";
    case SAVE_STACK:
        return L"Save Stack";
    case LOAD_STACK:
        return L"Saved Stacks";
    case MUTE_SLOT:
        return L"Mute voice";
    case SOLO_SLOT:
        return L"Solo voice";
    case REMOVE_SLOT:
        return L"Remove voice";
    case GAIN_SLOT:
        return L"Voice volume";
    case MASTER_GAIN:
        return L"Master volume";
    case EXPAND:
        return L"Now Playing";
    case HOME:
        return L"Return to Library";
    case SHRINK:
        return L"Mini Window";
    case CLOSE_PLAYER:
        return L"Close Now Playing";
    case FULLSCREEN:
        return L"Fullscreen";
    case FIT_GRAPH:
        return L"Fit graph";
    case SCATTER_GRAPH:
        return L"Reveal and arrange graph";
    case HISTORY_MENU:
        return L"Playback history";
    case PLAYLIST_MENU:
        return L"Playlists";
    case SHARE_FILES:
        return L"Share media or queue";
    case ALIGN_SOUND:
        return L"Align by sound";
    case OFFSET_LESS:
        return L"Offset minus 100 milliseconds";
    case OFFSET_MORE:
        return L"Offset plus 100 milliseconds";
    case OFFSET_RESET:
        return L"Reset offset";
    case PALETTE:
        return L"Color theme " + std::to_wstring(hit.data + 1);
    case ROW_SIZE:
        return L"Library row size " + std::to_wstring(hit.data + 1);
    case MINI_SIZE:
        return L"Mini Window size " + std::to_wstring(hit.data + 1);
    case REMOVE_FOLDER:
        return L"Remove Library folder";
    case OPEN_SOURCE_MENU:
        return L"Media actions";
    case SETTINGS_SECTION:
        return L"Settings section " + std::to_wstring(hit.data + 1);
    case NONE:
        return L"Resize pane";
    default:
        return L"Control";
    }
}
class NativeAccessible final : public IAccessible
{
    ULONG references = 1;
    HWND window;
    ComPtr<IAccessible> standard;
    LONG nativeCount = 0;
    Hit *child(VARIANT value)
    {
        if (value.vt != VT_I4 || value.lVal <= nativeCount)
            return nullptr;
        auto &list = hitsFor(window);
        LONG index = value.lVal - nativeCount - 1;
        return index < LONG(list.size()) ? &list[index] : nullptr;
    }

  public:
    explicit NativeAccessible(HWND h) : window(h)
    {
        CreateStdAccessibleObject(h, OBJID_CLIENT, IID_PPV_ARGS(&standard));
        if (standard)
            standard->get_accChildCount(&nativeCount);
    }
    HRESULT STDMETHODCALLTYPE QueryInterface(REFIID id, void **out) override
    {
        if (!out)
            return E_POINTER;
        *out = nullptr;
        if (id == IID_IUnknown || id == IID_IDispatch || id == IID_IAccessible)
        {
            *out = static_cast<IAccessible *>(this);
            AddRef();
            return S_OK;
        }
        return E_NOINTERFACE;
    }
    ULONG STDMETHODCALLTYPE AddRef() override
    {
        return ++references;
    }
    ULONG STDMETHODCALLTYPE Release() override
    {
        auto n = --references;
        if (!n)
            delete this;
        return n;
    }
    HRESULT STDMETHODCALLTYPE GetTypeInfoCount(UINT *n) override
    {
        if (!n)
            return E_POINTER;
        *n = 0;
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE GetTypeInfo(UINT, LCID, ITypeInfo **) override
    {
        return E_NOTIMPL;
    }
    HRESULT STDMETHODCALLTYPE GetIDsOfNames(REFIID, LPOLESTR *, UINT, LCID, DISPID *) override
    {
        return E_NOTIMPL;
    }
    HRESULT STDMETHODCALLTYPE Invoke(DISPID, REFIID, LCID, WORD, DISPPARAMS *, VARIANT *, EXCEPINFO *,
                                     UINT *) override
    {
        return E_NOTIMPL;
    }
    HRESULT STDMETHODCALLTYPE get_accParent(IDispatch **out) override
    {
        if (!out)
            return E_POINTER;
        *out = nullptr;
        return standard ? standard->get_accParent(out) : S_FALSE;
    }
    HRESULT STDMETHODCALLTYPE get_accChildCount(LONG *n) override
    {
        if (!n)
            return E_POINTER;
        *n = nativeCount + LONG(hitsFor(window).size());
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE get_accChild(VARIANT value, IDispatch **out) override
    {
        if (!out)
            return E_POINTER;
        *out = nullptr;
        if (value.vt == VT_I4 && value.lVal > 0 && value.lVal <= nativeCount && standard)
            return standard->get_accChild(value, out);
        return S_FALSE;
    }
    HRESULT STDMETHODCALLTYPE get_accName(VARIANT value, BSTR *out) override
    {
        if (!out)
            return E_POINTER;
        auto hit = child(value);
        if (!hit && value.vt == VT_I4 && value.lVal > 0 && standard)
            return standard->get_accName(value, out);
        *out = SysAllocString(hit ? translate(actionName(*hit), app.features.language).c_str()
                                  : L"Greater Art Desktop");
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE get_accValue(VARIANT value, BSTR *out) override
    {
        if (!out)
            return E_POINTER;
        *out = nullptr;
        auto hit = child(value);
        if (!hit)
            return S_FALSE;
        std::wstring text;
        if (hit->action == SEEK)
            text = formatTime(app.playback.time);
        else if (hit->action == SPEED)
            text = std::to_wstring(app.speed);
        else if (hit->action == MASTER_GAIN)
            text = std::to_wstring(app.master);
        else if (hit->action == GAIN_SLOT && hit->data < int(app.slots.size()))
            text = std::to_wstring(app.slots[hit->data].gain);
        else
            return S_FALSE;
        *out = SysAllocString(text.c_str());
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE get_accDescription(VARIANT value, BSTR *out) override
    {
        return get_accName(value, out);
    }
    HRESULT STDMETHODCALLTYPE get_accRole(VARIANT value, VARIANT *out) override
    {
        if (!out)
            return E_POINTER;
        auto hit = child(value);
        if (!hit && value.vt == VT_I4 && value.lVal > 0 && standard)
            return standard->get_accRole(value, out);
        VariantInit(out);
        out->vt = VT_I4;
        out->lVal = hit ? (hit->action == SEEK || hit->action == GAIN_SLOT || hit->action == MASTER_GAIN
                               ? ROLE_SYSTEM_SLIDER
                               : ROLE_SYSTEM_PUSHBUTTON)
                        : ROLE_SYSTEM_PANE;
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE get_accState(VARIANT value, VARIANT *out) override
    {
        if (!out)
            return E_POINTER;
        auto hit = child(value);
        if (!hit && value.vt == VT_I4 && value.lVal > 0 && standard)
            return standard->get_accState(value, out);
        VariantInit(out);
        out->vt = VT_I4;
        out->lVal = STATE_SYSTEM_FOCUSABLE;
        if (GetFocus() == window && value.vt == VT_I4 && value.lVal == nativeCount + app.keyboardFocus + 1)
            out->lVal |= STATE_SYSTEM_FOCUSED;
        if (app.locked && window == app.player && hit && hit->action != TRANSPORT_LOCK &&
            hit->action != FULLSCREEN && hit->action != CLOSE_PLAYER && hit->action != SHRINK &&
            hit->action != HOME)
            out->lVal |= STATE_SYSTEM_UNAVAILABLE;
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE get_accHelp(VARIANT, BSTR *out) override
    {
        if (out)
            *out = nullptr;
        return S_FALSE;
    }
    HRESULT STDMETHODCALLTYPE get_accHelpTopic(BSTR *out, VARIANT, LONG *topic) override
    {
        if (out)
            *out = nullptr;
        if (topic)
            *topic = 0;
        return S_FALSE;
    }
    HRESULT STDMETHODCALLTYPE get_accKeyboardShortcut(VARIANT, BSTR *out) override
    {
        if (out)
            *out = nullptr;
        return S_FALSE;
    }
    HRESULT STDMETHODCALLTYPE get_accFocus(VARIANT *out) override
    {
        if (!out)
            return E_POINTER;
        if (GetFocus() != window && standard)
            return standard->get_accFocus(out);
        VariantInit(out);
        out->vt = VT_I4;
        out->lVal = app.keyboardFocus >= 0 && app.keyboardFocus < int(hitsFor(window).size())
                        ? nativeCount + app.keyboardFocus + 1
                        : CHILDID_SELF;
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE get_accSelection(VARIANT *out) override
    {
        if (!out)
            return E_POINTER;
        VariantInit(out);
        return S_FALSE;
    }
    HRESULT STDMETHODCALLTYPE get_accDefaultAction(VARIANT value, BSTR *out) override
    {
        if (!out)
            return E_POINTER;
        *out = child(value) ? SysAllocString(L"Press") : nullptr;
        return *out ? S_OK : S_FALSE;
    }
    HRESULT STDMETHODCALLTYPE accSelect(LONG, VARIANT value) override
    {
        if (!child(value))
            return E_INVALIDARG;
        app.keyboardFocus = value.lVal - nativeCount - 1;
        SetFocus(window);
        invalidate();
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE accLocation(LONG *x, LONG *y, LONG *width, LONG *height, VARIANT value) override
    {
        if (!x || !y || !width || !height)
            return E_POINTER;
        RECT rect{};
        auto hit = child(value);
        if (hit)
        {
            rect = physical(hit->box);
            POINT point{rect.left, rect.top};
            ClientToScreen(window, &point);
            *x = point.x;
            *y = point.y;
            *width = rect.right - rect.left;
            *height = rect.bottom - rect.top;
        }
        else
        {
            GetWindowRect(window, &rect);
            *x = rect.left;
            *y = rect.top;
            *width = rect.right - rect.left;
            *height = rect.bottom - rect.top;
        }
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE accNavigate(LONG direction, VARIANT from, VARIANT *out) override
    {
        if (!out)
            return E_POINTER;
        VariantInit(out);
        if (from.vt != VT_I4)
            return E_INVALIDARG;
        LONG count = nativeCount + LONG(hitsFor(window).size());
        LONG index = from.lVal + (direction == NAVDIR_PREVIOUS ? -1 : 1);
        if (direction == NAVDIR_FIRSTCHILD)
            index = 1;
        else if (direction == NAVDIR_LASTCHILD)
            index = count;
        else if (direction != NAVDIR_NEXT && direction != NAVDIR_PREVIOUS)
            return S_FALSE;
        if (index < 1 || index > count)
            return S_FALSE;
        out->vt = VT_I4;
        out->lVal = index;
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE accHitTest(LONG x, LONG y, VARIANT *out) override
    {
        if (!out)
            return E_POINTER;
        VariantInit(out);
        POINT point{x, y};
        ScreenToClient(window, &point);
        auto &list = hitsFor(window);
        for (std::size_t i = 0; i < list.size(); ++i)
            if (list[i].box.contains(logical(point.x), logical(point.y)))
            {
                out->vt = VT_I4;
                out->lVal = nativeCount + LONG(i + 1);
                return S_OK;
            }
        return standard ? standard->accHitTest(x, y, out) : S_FALSE;
    }
    HRESULT STDMETHODCALLTYPE accDoDefaultAction(VARIANT value) override
    {
        auto hit = child(value);
        if (!hit)
            return E_INVALIDARG;
        action(*hit, window);
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE put_accName(VARIANT, BSTR) override
    {
        return E_NOTIMPL;
    }
    HRESULT STDMETHODCALLTYPE put_accValue(VARIANT value, BSTR text) override
    {
        auto hit = child(value);
        if (!hit || !text)
            return E_INVALIDARG;
        if (app.locked && window == app.player)
            return E_ACCESSDENIED;
        wchar_t *end = nullptr;
        double number = wcstod(text, &end);
        if (end == text || *end || !std::isfinite(number))
            return E_INVALIDARG;
        if (hit->action == SEEK)
        {
            seek(std::clamp(number, 0.0, app.playback.duration));
            return S_OK;
        }
        if (hit->action == MASTER_GAIN)
        {
            app.master = int(std::clamp(number, 0.0, 100.0));
            gains();
            invalidate();
            return S_OK;
        }
        if (hit->action == GAIN_SLOT && hit->data >= 0 && hit->data < int(app.slots.size()))
        {
            app.slots[hit->data].gain = int(std::clamp(number, 0.0, 100.0));
            gains();
            invalidate();
            return S_OK;
        }
        return E_NOTIMPL;
    }
};
LONG accessibleChildId(HWND window, int index)
{
    ComPtr<IAccessible> standard;
    LONG count = 0;
    if (SUCCEEDED(CreateStdAccessibleObject(window, OBJID_CLIENT, IID_PPV_ARGS(&standard))))
        standard->get_accChildCount(&count);
    return count + index + 1;
}
bool probeAccessibility(HWND window)
{
    ComPtr<IAccessible> accessible;
    accessible.Attach(new NativeAccessible(window));
    LONG count = 0;
    if (FAILED(accessible->get_accChildCount(&count)) || count < LONG(hitsFor(window).size()))
        return false;
    for (auto &hit : hitsFor(window))
    {
        if (hit.box.w <= 0 || hit.box.h <= 0)
            continue;
        POINT point{px(hit.box.x + hit.box.w / 2), px(hit.box.y + hit.box.h / 2)};
        ClientToScreen(window, &point);
        VARIANT child;
        VariantInit(&child);
        BSTR name = nullptr;
        bool okay = SUCCEEDED(accessible->accHitTest(point.x, point.y, &child)) && child.vt == VT_I4 &&
                    child.lVal > 0 && SUCCEEDED(accessible->get_accName(child, &name)) && name &&
                    SysStringLen(name) > 0;
        SysFreeString(name);
        VariantClear(&child);
        if (!okay)
            return false;
    }
    return true;
}
void inspectElement(HWND owner, const Hit &hit)
{
    std::wostringstream report;
    report << L"Element: " << actionName(hit) << L"\r\nPresentation: "
           << (owner == app.player ? L"Now Playing"
               : owner == app.mini ? L"Mini"
                                   : L"Workspace")
           << L"\r\nBounds (logical pixels): " << hit.box.x << L", " << hit.box.y << L" · " << hit.box.w
           << L" × " << hit.box.h << L"\r\nAction: " << int(hit.action) << L"\r\nPlayback: "
           << app.playback.state << L"\r\nVersion: " << VERSION;
    auto content = report.str();
    std::ofstream log(app.dataDir / L"inspector-latest.txt", std::ios::binary | std::ios::trunc);
    log << toUtf8(content);
    showDetails(L"Element inspector", content);
}
LRESULT accessibilityObject(HWND window, WPARAM w, LPARAM l)
{
    if (LONG(l) != OBJID_CLIENT)
        return 0;
    auto object = new NativeAccessible(window);
    auto result = LresultFromObject(IID_IAccessible, w, object);
    object->Release();
    return result;
}
void accessibleName(HWND window, const wchar_t *name)
{
    ComPtr<IAccPropServices> services;
    if (SUCCEEDED(
            CoCreateInstance(CLSID_AccPropServices, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&services))))
        services->SetHwndPropStr(window, static_cast<DWORD>(OBJID_CLIENT), CHILDID_SELF, PROPID_ACC_NAME,
                                 name);
}
} // namespace
