namespace
{
winrt::Windows::ApplicationModel::DataTransfer::DataTransferManager shareManager{nullptr};
winrt::event_token shareToken{};
std::vector<winrt::Windows::Storage::IStorageItem> shareItems;
std::mutex shareMutex;
std::wstring shareError;
constexpr UINT SHARE_READY = WM_APP + 19;
void prepareShare(std::vector<std::wstring> paths)
{
    if (paths.empty() || paths.size() > 1000)
        return;
    bool accepted = app.scans.emplace_back([paths] {
        std::vector<winrt::Windows::Storage::IStorageItem> items;
        std::wstring error;
        try
        {
            struct Apartment
            {
                Apartment()
                {
                    winrt::init_apartment(winrt::apartment_type::multi_threaded);
                }
                ~Apartment()
                {
                    winrt::uninit_apartment();
                }
            } apartment;
            for (auto &path : paths)
            {
                if (!localMediaPath(path))
                    throw std::runtime_error("Non-local share");
                items.push_back(winrt::Windows::Storage::StorageFile::GetFileFromPathAsync(path).get());
            }
        }
        catch (const winrt::hresult_error &e)
        {
            error = std::wstring(e.message());
        }
        catch (...)
        {
            error = L"These local files could not be shared.";
        }
        {
            std::lock_guard<std::mutex> lock(shareMutex);
            shareItems = std::move(items);
            shareError = std::move(error);
        }
        PostMessage(app.main, SHARE_READY, 0, 0);
    });
    if (!accepted)
        toast(L"Share preparation is busy. Try again shortly.");
}
void showWindowsShare(HWND owner)
{
    try
    {
        {
            std::lock_guard<std::mutex> lock(shareMutex);
            if (!shareError.empty())
            {
                toast(shareError);
                return;
            }
        }
        auto interop =
            winrt::get_activation_factory<winrt::Windows::ApplicationModel::DataTransfer::DataTransferManager,
                                          IDataTransferManagerInterop>();
        if (shareManager)
            shareManager.DataRequested(shareToken);
        winrt::check_hresult(interop->GetForWindow(
            owner, winrt::guid_of<winrt::Windows::ApplicationModel::DataTransfer::DataTransferManager>(),
            winrt::put_abi(shareManager)));
        shareToken = shareManager.DataRequested(
            [](winrt::Windows::ApplicationModel::DataTransfer::DataTransferManager const &,
               winrt::Windows::ApplicationModel::DataTransfer::DataRequestedEventArgs const &args) {
                auto data = args.Request().Data();
                data.Properties().Title(L"Greater Art");
                data.Properties().Description(L"Original local media / playlist");
                std::lock_guard<std::mutex> lock(shareMutex);
                auto files = winrt::single_threaded_vector<winrt::Windows::Storage::IStorageItem>();
                for (auto &file : shareItems)
                    files.Append(file);
                data.SetStorageItems(files);
            });
        winrt::check_hresult(interop->ShowShareUIForWindow(owner));
    }
    catch (const winrt::hresult_error &e)
    {
        toast(L"Windows Share is unavailable: " + std::wstring(e.message()));
    }
}
void shareMenu(HWND owner)
{
    if (app.slots.empty())
        return;
    auto menu = CreatePopupMenu();
    AppendMenu(menu, MF_STRING, 1, L"Share original current file");
    AppendMenu(menu, MF_STRING, 2, L"Share queue as M3U8…");
    AppendMenu(menu, MF_STRING, 3, L"Share original queue files");
    POINT p{};
    GetCursorPos(&p);
    int choice = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, p.x, p.y, 0, owner, nullptr);
    DestroyMenu(menu);
    if (choice == 1)
        prepareShare({app.slots[0].song.path});
    if (choice == 2)
    {
        auto path = chooseTextFile(true);
        if (!path.empty() && exportPlaylist(path, visiblePlaylist()))
            prepareShare({path});
        else if (!path.empty())
            toast(L"Choose a new export filename.");
    }
    if (choice == 3)
        prepareShare(visiblePlaylist().paths);
}
} // namespace
