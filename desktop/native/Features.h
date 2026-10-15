#pragma once
#include "Core.h"
#include <ctime>
#include <limits>
#include <sstream>

namespace ga
{
struct Playlist
{
    std::wstring name;
    std::vector<std::wstring> paths;
};
struct History
{
    std::wstring path;
    std::int64_t at = 0;
};
struct UserFeatures
{
    std::vector<Playlist> playlists;
    std::vector<History> history;
    bool historyEnabled = false, resume = true, disc = false, developer = false;
    bool light = false, highContrast = false, shuffle = false;
    int language = 0, background = 2, scale = 0, repeat = 1, font = 0;
    int graphNeighbors = 6, graphThreshold = 8;
    std::wstring lastPath, customImage;
    double lastPosition = 0, speed = 1;
};
struct Lyric
{
    double time = 0;
    std::wstring text;
};
inline std::wstring fromUtf8(const std::string &bytes)
{
    if (bytes.empty())
        return {};
    int count =
        MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, bytes.data(), int(bytes.size()), nullptr, 0);
    if (!count)
        return {};
    std::wstring text(count, L'\0');
    MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, bytes.data(), int(bytes.size()), text.data(), count);
    if (!text.empty() && text[0] == 0xfeff)
        text.erase(text.begin());
    return text;
}
inline std::string toUtf8(const std::wstring &text)
{
    if (text.empty())
        return {};
    int count = WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, text.data(), int(text.size()), nullptr, 0,
                                    nullptr, nullptr);
    std::string bytes(count, '\0');
    if (count)
        WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, text.data(), int(text.size()), bytes.data(), count,
                            nullptr, nullptr);
    return bytes;
}
inline std::wstring readLocalText(const std::filesystem::path &file, unsigned limit = 1024 * 1024)
{
    std::error_code error;
    auto count = std::filesystem::file_size(file, error);
    if (error || count > limit)
        return {};
    std::ifstream input(file, std::ios::binary);
    std::string bytes(static_cast<std::size_t>(count), '\0');
    input.read(bytes.data(), std::streamsize(count));
    return input ? fromUtf8(bytes) : std::wstring{};
}
inline void pruneWaveCache(const std::filesystem::path &directory)
{
    struct Cached
    {
        std::filesystem::path file;
        std::uintmax_t size;
        std::filesystem::file_time_type modified;
    };
    std::vector<Cached> files;
    std::uintmax_t total = 0;
    std::error_code error;
    for (std::filesystem::directory_iterator i(directory, error), end; i != end && !error; i.increment(error))
    {
        auto name = i->path().filename().wstring();
        if (name.size() < 10 || name.rfind(L"wave-", 0) != 0 || name.substr(name.size() - 4) != L".bin")
            continue;
        auto digits = name.substr(5, name.size() - 9);
        if (digits.empty() || digits.find_first_not_of(L"0123456789") != std::wstring::npos)
            continue;
        if (i->is_symlink(error) || !i->is_regular_file(error) || error)
        {
            error.clear();
            continue;
        }
        auto size = i->file_size(error);
        if (error)
        {
            error.clear();
            continue;
        }
        auto time = i->last_write_time(error);
        if (error)
        {
            error.clear();
            continue;
        }
        files.push_back({i->path(), size, time});
        total += size;
    }
    std::sort(files.begin(), files.end(), [](auto &a, auto &b) { return a.modified < b.modified; });
    std::size_t count = files.size();
    for (auto &entry : files)
    {
        if (total <= 128 * 1024 * 1024 && count <= 128)
            break;
        if (std::filesystem::remove(entry.file, error))
        {
            total -= entry.size;
            --count;
        }
        error.clear();
    }
}
inline bool localMediaPath(const std::wstring &path)
{
    if (path.empty() || path.rfind(L"\\\\", 0) == 0 || path.find(L"://") != std::wstring::npos)
        return false;
    auto colon = path.find(L':');
    return colon == std::wstring::npos ||
           (colon == 1 && iswalpha(path[0]) && path.find(L':', 2) == std::wstring::npos);
}
inline Playlist importPlaylist(const std::filesystem::path &file)
{
    Playlist list{file.stem().wstring(), {}};
    std::wistringstream input(readLocalText(file));
    std::wstring line;
    while (std::getline(input, line) && list.paths.size() < 100000)
    {
        if (!line.empty() && line.back() == L'\r')
            line.pop_back();
        if (line.empty() || line.front() == L'#' || !localMediaPath(line))
            continue;
        auto path = std::filesystem::path(line);
        if (path.is_relative())
            path = file.parent_path() / path;
        std::error_code error;
        if (localMediaPath(path.wstring()) && mediaExtension(path.extension().wstring()) &&
            std::filesystem::is_regular_file(path, error))
            list.paths.push_back(canonicalPath(path.wstring()));
    }
    return list;
}
inline bool exportPlaylist(const std::filesystem::path &file, const Playlist &list)
{
    if (!localMediaPath(file.wstring()) || list.paths.size() > 100000)
        return false;
    for (auto &path : list.paths)
        if (!localMediaPath(path) || path.find_first_of(L"\r\n") != std::wstring::npos)
            return false;
    std::error_code error;
    std::ostringstream output;
    output << "#EXTM3U\n";
    for (auto &path : list.paths)
    {
        auto relative = std::filesystem::relative(path, file.parent_path(), error);
        output << toUtf8(error ? path : relative.wstring()) << '\n';
        error.clear();
    }
    auto bytes = output.str();
    if (bytes.size() > 16 * 1024 * 1024)
        return false;
    // CREATE_NEW closes the check/open race; exports never replace user files.
    HANDLE handle =
        CreateFile(file.c_str(), GENERIC_WRITE, 0, nullptr, CREATE_NEW, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (handle == INVALID_HANDLE_VALUE)
        return false;
    DWORD written = 0;
    bool okay = WriteFile(handle, bytes.data(), DWORD(bytes.size()), &written, nullptr) &&
                written == bytes.size() && FlushFileBuffers(handle);
    CloseHandle(handle);
    if (!okay)
        DeleteFile(file.c_str());
    return okay;
}
inline std::vector<Lyric> parseLyrics(const std::wstring &source)
{
    std::wistringstream input(source);
    std::vector<Lyric> lyrics;
    std::wstring line;
    double offset = 0;
    while (std::getline(input, line) && lyrics.size() < 20000)
    {
        if (line.rfind(L"[offset:", 0) == 0)
        {
            wchar_t *end = nullptr;
            auto number = wcstod(line.c_str() + 8, &end);
            if (end && *end == L']' && std::isfinite(number) && std::abs(number) < 3600000)
                offset = number / 1000;
            continue;
        }
        std::vector<double> stamps;
        std::size_t position = 0;
        while (position < line.size() && line[position] == L'[')
        {
            auto end = line.find(L']', position);
            if (end == std::wstring::npos)
                break;
            auto tag = line.substr(position + 1, end - position - 1);
            auto colon = tag.find(L':');
            if (colon == std::wstring::npos)
                break;
            wchar_t *a = nullptr, *b = nullptr;
            auto minutes = wcstod(tag.c_str(), &a), seconds = wcstod(tag.c_str() + colon + 1, &b);
            if (!a || a != tag.c_str() + colon || !b || *b || !std::isfinite(minutes) ||
                !std::isfinite(seconds) || minutes < 0 || seconds < 0 || seconds >= 60)
                break;
            stamps.push_back(minutes * 60 + seconds);
            position = end + 1;
        }
        if (!stamps.empty())
            for (auto stamp : stamps)
                lyrics.push_back({stamp, line.substr(position)});
    }
    for (auto &lyric : lyrics)
        lyric.time = (std::max)(0.0, lyric.time + offset);
    std::stable_sort(lyrics.begin(), lyrics.end(), [](auto &a, auto &b) { return a.time < b.time; });
    return lyrics;
}
inline int activeLyric(const std::vector<Lyric> &lyrics, double position)
{
    auto i = std::upper_bound(lyrics.begin(), lyrics.end(), position,
                              [](double time, auto &line) { return time < line.time; });
    return i == lyrics.begin() ? -1 : int(i - lyrics.begin() - 1);
}
inline void recordHistory(UserFeatures &features, const std::wstring &path, std::int64_t at)
{
    if (!features.historyEnabled)
        return;
    features.history.push_back({path, at});
    if (features.history.size() > 2000)
        features.history.erase(features.history.begin(),
                               features.history.begin() + features.history.size() - 2000);
}
inline bool saveFeatures(const std::filesystem::path &directory, const UserFeatures &features)
{
    auto temporary = directory / L"features-v1.tmp", file = directory / L"features-v1.bin";
    std::ofstream output(temporary, std::ios::binary);
    output.write("GAFEAT01", 8);
    for (int n : {int(features.historyEnabled), int(features.resume), int(features.disc),
                  int(features.developer), int(features.light), int(features.highContrast),
                  int(features.shuffle), features.language, features.background, features.scale,
                  features.repeat, features.font, features.graphNeighbors, features.graphThreshold})
        output.write(reinterpret_cast<char *>(&n), 4);
    writeString(output, features.lastPath);
    writeString(output, features.customImage);
    output.write(reinterpret_cast<const char *>(&features.lastPosition), 8);
    output.write(reinterpret_cast<const char *>(&features.speed), 8);
    auto count = std::uint32_t(features.playlists.size());
    output.write(reinterpret_cast<char *>(&count), 4);
    for (auto &list : features.playlists)
    {
        writeString(output, list.name);
        count = std::uint32_t(list.paths.size());
        output.write(reinterpret_cast<char *>(&count), 4);
        for (auto &path : list.paths)
            writeString(output, path);
    }
    count = features.historyEnabled ? std::uint32_t(features.history.size()) : 0;
    output.write(reinterpret_cast<char *>(&count), 4);
    if (features.historyEnabled)
        for (auto &entry : features.history)
        {
            writeString(output, entry.path);
            output.write(reinterpret_cast<const char *>(&entry.at), 8);
        }
    output.close();
    return output &&
           MoveFileEx(temporary.c_str(), file.c_str(), MOVEFILE_REPLACE_EXISTING | MOVEFILE_WRITE_THROUGH);
}
inline UserFeatures loadFeatures(const std::filesystem::path &directory)
{
    std::error_code error;
    auto file = directory / L"features-v1.bin";
    if (std::filesystem::file_size(file, error) > 32 * 1024 * 1024 || error)
        return {};
    std::ifstream input(file, std::ios::binary);
    char magic[8]{};
    input.read(magic, 8);
    if (std::string(magic, 8) != "GAFEAT01")
        return {};
    int n[14]{};
    input.read(reinterpret_cast<char *>(n), sizeof(n));
    UserFeatures f;
    f.historyEnabled = n[0] != 0;
    f.resume = n[1] != 0;
    f.disc = n[2] != 0;
    f.developer = n[3] != 0;
    f.light = n[4] != 0;
    f.highContrast = n[5] != 0;
    f.shuffle = n[6] != 0;
    f.language = std::clamp(n[7], 0, 5);
    f.background = std::clamp(n[8], 0, 3);
    f.scale = std::clamp(n[9], 0, 1);
    f.repeat = std::clamp(n[10], 0, 2);
    f.font = std::clamp(n[11], 0, 3);
    f.graphNeighbors = std::clamp(n[12], 2, 8);
    f.graphThreshold = std::clamp(n[13], 0, 80);
    if (!readString(input, f.lastPath) || !readString(input, f.customImage))
        return {};
    input.read(reinterpret_cast<char *>(&f.lastPosition), 8);
    input.read(reinterpret_cast<char *>(&f.speed), 8);
    if (!std::isfinite(f.lastPosition) || f.lastPosition < 0 || !std::isfinite(f.speed) || f.speed < .25 ||
        f.speed > 4)
        return {};
    auto count = [&](unsigned limit) {
        std::uint32_t value = 0;
        input.read(reinterpret_cast<char *>(&value), 4);
        return input && value <= limit ? int(value) : -1;
    };
    int lists = count(1000);
    if (lists < 0)
        return {};
    for (int i = 0; i < lists; ++i)
    {
        Playlist list;
        if (!readString(input, list.name))
            return {};
        int paths = count(100000);
        if (paths < 0)
            return {};
        for (int j = 0; j < paths; ++j)
        {
            std::wstring path;
            if (!readString(input, path) || !localMediaPath(path))
                return {};
            list.paths.push_back(path);
        }
        f.playlists.push_back(std::move(list));
    }
    int history = count(2000);
    if (history < 0)
        return {};
    for (int i = 0; i < history; ++i)
    {
        History entry;
        if (!readString(input, entry.path))
            return {};
        input.read(reinterpret_cast<char *>(&entry.at), 8);
        if (f.historyEnabled)
            f.history.push_back(std::move(entry));
    }
    return input ? f : UserFeatures{};
}
struct Practice
{
    double a = -1, b = -1;
    void mark(double time)
    {
        if (a < 0)
            a = time;
        else if (b < 0 && time > a + .25)
            b = time;
        else
        {
            a = b = -1;
        }
    }
    bool shouldLoop(double time) const
    {
        return a >= 0 && b > a && time >= b;
    }
};
inline double correctionRate(double drift)
{
    if (!std::isfinite(drift) || std::abs(drift) < .012)
        return 1;
    return std::clamp(1 - drift * .12, .98, 1.02);
}
} // namespace ga
