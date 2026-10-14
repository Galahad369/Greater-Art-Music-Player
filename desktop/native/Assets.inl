namespace
{
void loadCustomBackground()
{
    app.customBackground.reset();
    auto path = app.features.customImage;
    if (path.empty() || !localMediaPath(path))
        return;
    std::error_code error;
    if (std::filesystem::file_size(path, error) > 64 * 1024 * 1024 || error)
    {
        toast(L"Background image is unavailable or too large.");
        return;
    }
    auto image = std::shared_ptr<Gdiplus::Bitmap>(Gdiplus::Bitmap::FromFile(path.c_str()));
    if (image && image->GetLastStatus() == Gdiplus::Ok && image->GetWidth() <= 8192 &&
        image->GetHeight() <= 8192)
        app.customBackground = std::move(image);
    else
        toast(L"Background image could not be decoded.");
}
void requestAssets(const Song &song)
{
    unsigned generation = ++app.assetGeneration;
    app.artwork.reset();
    app.waveform.clear();
    app.artist.clear();
    app.album.clear();
    app.scans.emplace_back([song, generation] {
        SetThreadPriority(GetCurrentThread(), THREAD_PRIORITY_BELOW_NORMAL);
        auto cancelled = [generation] { return app.closing || app.assetGeneration != generation; };
        App::AssetResult result;
        result.generation = generation;
        struct Apartment
        {
            HRESULT hr = CoInitializeEx(nullptr, COINIT_MULTITHREADED);
            ~Apartment()
            {
                if (SUCCEEDED(hr))
                    CoUninitialize();
            }
        } apartment;
        if (SUCCEEDED(apartment.hr))
        {
            ComPtr<IPropertyStore> properties;
            if (SUCCEEDED(SHGetPropertyStoreFromParsingName(song.path.c_str(), nullptr, GPS_DEFAULT,
                                                            IID_PPV_ARGS(&properties))))
            {
                auto read = [&](const PROPERTYKEY &key) {
                    PROPVARIANT value;
                    PropVariantInit(&value);
                    std::wstring text;
                    if (SUCCEEDED(properties->GetValue(key, &value)))
                    {
                        PWSTR buffer = nullptr;
                        if (SUCCEEDED(PropVariantToStringAlloc(value, &buffer)) && buffer)
                        {
                            text = buffer;
                            CoTaskMemFree(buffer);
                        }
                    }
                    PropVariantClear(&value);
                    return text;
                };
                result.artist = read(PKEY_Music_Artist);
                result.album = read(PKEY_Music_AlbumTitle);
            }
            ComPtr<IShellItemImageFactory> factory;
            HBITMAP bitmap = nullptr;
            if (!cancelled() &&
                SUCCEEDED(SHCreateItemFromParsingName(song.path.c_str(), nullptr, IID_PPV_ARGS(&factory))) &&
                SUCCEEDED(factory->GetImage({320, 320}, SIIGBF_THUMBNAILONLY, &bitmap)) && bitmap)
            {
                auto image = std::shared_ptr<Gdiplus::Bitmap>(Gdiplus::Bitmap::FromHBITMAP(bitmap, nullptr));
                DeleteObject(bitmap);
                if (image && image->GetLastStatus() == Gdiplus::Ok)
                {
                    int totals[2][4]{};
                    for (int y = 0; y < 16; ++y)
                        for (int x = 0; x < 16; ++x)
                        {
                            Gdiplus::Color c;
                            image->GetPixel(x * image->GetWidth() / 16, y * image->GetHeight() / 16, &c);
                            if (c.GetR() + c.GetG() + c.GetB() > 45)
                            {
                                auto &s = totals[y < 8 ? 0 : 1];
                                s[0] += c.GetR();
                                s[1] += c.GetG();
                                s[2] += c.GetB();
                                ++s[3];
                            }
                        }
                    for (int i = 0; i < 2; ++i)
                        if (totals[i][3])
                            result.ambient[i] = RGB(totals[i][0] / totals[i][3], totals[i][1] / totals[i][3],
                                                    totals[i][2] / totals[i][3]);
                    result.artwork = std::move(image);
                }
            }
        }
        if (!cancelled())
        {
            {
                std::lock_guard<std::mutex> lock(app.assetMutex);
                app.assetResults.push_back(result);
            }
            PostMessage(app.main, ASSET_READY, 0, 0);
        }
        // Analysis cache keys use the real local file identity/size/mtime, not a
        // playback position. Only the selected track is decoded in the background.
        Song key = song;
        std::error_code error;
        key.bytes = std::filesystem::file_size(song.path, error);
        key.modified = std::filesystem::last_write_time(song.path, error).time_since_epoch().count();
        auto file = app.dataDir / (L"wave-" + std::to_wstring(graphSignature({key})) + L".bin");
        std::ifstream cached(file, std::ios::binary);
        std::uint32_t count = 0;
        char magic[8]{};
        cached.read(magic, 8);
        cached.read(reinterpret_cast<char *>(&count), 4);
        bool valid = cached && std::string(magic, 8) == "GAWAVE01" && count <= 2160000;
        if (valid)
        {
            result.waveform.resize(count);
            cached.read(reinterpret_cast<char *>(result.waveform.data()), count * sizeof(float));
            valid = bool(cached) && std::all_of(result.waveform.begin(), result.waveform.end(), [](float n) {
                        return std::isfinite(n) && n >= 0 && n < 1e6;
                    });
        }
        if (!valid && !cancelled())
        {
            while (!cancelled() && !app.assetPermit)
                Sleep(25);
            if (cancelled())
                return;
            auto budgetCancelled = [&] {
                while (!cancelled() && !app.assetPermit)
                    Sleep(25);
                return cancelled();
            };
            auto analysed = analyseAudio(song.path, budgetCancelled, 43200);
            result.waveform = std::move(analysed.energy);
            if (analysed.error.empty() && !cancelled())
            {
                auto temporary = file;
                temporary += L".tmp";
                std::ofstream output(temporary, std::ios::binary);
                count = std::uint32_t(result.waveform.size());
                output.write("GAWAVE01", 8);
                output.write(reinterpret_cast<char *>(&count), 4);
                output.write(reinterpret_cast<const char *>(result.waveform.data()), count * sizeof(float));
                output.close();
                if (output)
                    MoveFileEx(temporary.c_str(), file.c_str(),
                               MOVEFILE_REPLACE_EXISTING | MOVEFILE_WRITE_THROUGH);
                pruneWaveCache(app.dataDir);
            }
        }
        if (cancelled())
            return;
        {
            std::lock_guard<std::mutex> lock(app.assetMutex);
            app.assetResults.push_back(std::move(result));
        }
        PostMessage(app.main, ASSET_READY, 0, 0);
    });
}
void assetReady()
{
    std::vector<App::AssetResult> results;
    {
        std::lock_guard<std::mutex> lock(app.assetMutex);
        results.swap(app.assetResults);
    }
    for (auto &result : results)
        if (result.generation == app.assetGeneration)
        {
            app.artwork = std::move(result.artwork);
            app.waveform = std::move(result.waveform);
            app.wavePeak =
                app.waveform.empty() ? 0 : *std::max_element(app.waveform.begin(), app.waveform.end());
            app.artist = std::move(result.artist);
            app.album = std::move(result.album);
            app.ambient[0] = result.ambient[0];
            app.ambient[1] = result.ambient[1];
        }
    invalidate();
    if (app.preview)
        InvalidateRect(app.preview, nullptr, FALSE);
}
} // namespace
