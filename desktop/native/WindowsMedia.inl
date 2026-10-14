namespace
{
constexpr UINT SYSTEM_MEDIA = WM_APP + 25;
winrt::Windows::Media::SystemMediaTransportControls systemMedia{nullptr};
winrt::event_token mediaButtons{};
void initializeSystemMedia()
{
    try
    {
        auto interop = winrt::get_activation_factory<winrt::Windows::Media::SystemMediaTransportControls,
                                                     ISystemMediaTransportControlsInterop>();
        winrt::check_hresult(
            interop->GetForWindow(app.main, winrt::guid_of<winrt::Windows::Media::SystemMediaTransportControls>(),
                                  winrt::put_abi(systemMedia)));
        systemMedia.IsEnabled(true);
        systemMedia.IsPlayEnabled(true);
        systemMedia.IsPauseEnabled(true);
        systemMedia.IsNextEnabled(true);
        systemMedia.IsPreviousEnabled(true);
        mediaButtons = systemMedia.ButtonPressed(
            [](auto const &, auto const &event) { PostMessage(app.main, SYSTEM_MEDIA, WPARAM(event.Button()), 0); });
    }
    catch (const winrt::hresult_error &e)
    {
        systemMedia = nullptr;
        OutputDebugString((L"System media controls: " + std::wstring(e.message()) + L"\n").c_str());
    }
}
void publishSystemMedia()
{
    if (!systemMedia)
        return;
    try
    {
        auto status = app.playback.state == 3 ? winrt::Windows::Media::MediaPlaybackStatus::Playing
                      : app.slots.empty()     ? winrt::Windows::Media::MediaPlaybackStatus::Stopped
                                              : winrt::Windows::Media::MediaPlaybackStatus::Paused;
        if (systemMedia.PlaybackStatus() != status)
            systemMedia.PlaybackStatus(status);
        static std::wstring previous;
        auto identity = app.slots.empty() ? L"" : app.slots[0].song.path + L"\n" + app.artist;
        if (previous != identity)
        {
            previous = identity;
            auto display = systemMedia.DisplayUpdater();
            if (app.slots.empty())
                display.ClearAll();
            else
            {
                display.Type(winrt::Windows::Media::MediaPlaybackType::Music);
                display.MusicProperties().Title(app.slots[0].song.title);
                display.MusicProperties().Artist(app.artist);
            }
            display.Update();
        }
    }
    catch (const winrt::hresult_error &)
    {
    }
}
void removeSystemMedia()
{
    if (systemMedia)
    {
        try
        {
            systemMedia.ButtonPressed(mediaButtons);
            systemMedia.IsEnabled(false);
        }
        catch (...)
        {
        }
        systemMedia = nullptr;
    }
    if (shareManager)
    {
        try
        {
            shareManager.DataRequested(shareToken);
        }
        catch (...)
        {
        }
        shareManager = nullptr;
    }
    shareItems.clear();
}
} // namespace
