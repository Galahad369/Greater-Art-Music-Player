#pragma once
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <functional>
#include <set>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <vector>
#include <windows.h>

namespace ga
{
inline constexpr wchar_t VERSION[] = L"0.1.12";
inline constexpr int VERSION_CODE = 13;
struct Song
{
    std::wstring path, title, format;
    std::uint64_t bytes = 0;
    std::int64_t modified = 0;
    bool video = false;
};
struct SavedTrack
{
    std::wstring path;
    int gain = 100;
    bool mute = false, solo = false;
    double offset = 0;
};
struct SavedStack
{
    std::wstring name;
    std::vector<SavedTrack> tracks;
};
struct Settings
{
    std::vector<std::wstring> roots;
    std::set<std::wstring> favorites;
    std::vector<SavedStack> stacks;
    int palette = 0, row = 0, miniSize = 0, sort = 0;
    bool loop = true;
    double leftWidth = 280, rightWidth = 320;
};
struct Point
{
    double x = 0, y = 0;
};
struct Edge
{
    std::size_t a = 0, b = 0;
    double strength = 0;
};
struct Graph
{
    std::vector<Point> points;
    std::vector<Edge> edges;
    std::vector<double> weights;
};
inline std::wstring lower(std::wstring value)
{
    if (!value.empty())
        CharLowerBuff(value.data(), DWORD(value.size()));
    return value;
}
inline std::wstring titleOf(const std::wstring &path)
{
    return std::filesystem::path(path).stem().wstring();
}
inline std::wstring canonicalPath(const std::wstring &path)
{
    return std::filesystem::absolute(path).lexically_normal().make_preferred().wstring();
}
inline bool mediaExtension(std::wstring extension)
{
    extension = lower(extension);
    static const std::set<std::wstring> all{L".mp3", L".mp4",  L".m4a", L".flac", L".wav", L".aac", L".ogg", L".opus",
                                            L".mkv", L".webm", L".avi", L".mov",  L".m4v", L".wma", L".aiff"};
    return all.count(extension) != 0;
}
inline std::vector<Song> scanLibrary(const std::vector<std::wstring> &roots, const std::function<bool()> &canceled)
{
    std::vector<Song> songs;
    std::set<std::wstring> seen;
    for (const auto &root : roots)
    {
        if (root.rfind(L"\\\\", 0) == 0)
            continue;
        std::error_code error;
        auto iterator = std::filesystem::recursive_directory_iterator(
            root, std::filesystem::directory_options::skip_permission_denied, error);
        while (!error && iterator != std::filesystem::recursive_directory_iterator())
        {
            if (canceled())
                return {};
            const auto entry = *iterator;
            auto attributes = GetFileAttributes(entry.path().c_str());
            if (attributes != INVALID_FILE_ATTRIBUTES && attributes & FILE_ATTRIBUTE_REPARSE_POINT)
                iterator.disable_recursion_pending();
            else if (entry.is_regular_file(error) && mediaExtension(entry.path().extension().wstring()))
            {
                auto path = std::filesystem::absolute(entry.path(), error).lexically_normal().wstring();
                if (!error && seen.insert(lower(path)).second)
                {
                    Song song;
                    song.path = path;
                    song.title = titleOf(path);
                    song.format = entry.path().extension().wstring().substr(1);
                    CharUpperBuff(song.format.data(), DWORD(song.format.size()));
                    song.bytes = entry.file_size(error);
                    song.modified = entry.last_write_time(error).time_since_epoch().count();
                    auto extension = lower(entry.path().extension().wstring());
                    song.video = extension == L".mp4" || extension == L".mkv" || extension == L".webm" ||
                                 extension == L".avi" || extension == L".mov" || extension == L".m4v";
                    if (!error)
                        songs.push_back(std::move(song));
                }
            }
            error.clear();
            iterator.increment(error);
        }
    }
    return songs;
}

struct Features
{
    std::set<std::u32string> words, grams;
    std::set<char32_t> chars;
    std::u32string text;
};
inline Features filenameFeatures(const std::wstring &raw)
{
    Features result;
    auto text = lower(std::filesystem::path(raw).stem().wstring());
    std::u32string word;
    for (std::size_t i = 0; i < text.size(); ++i)
    {
        char32_t c = text[i];
        if (c >= 0xd800 && c <= 0xdbff && i + 1 < text.size() && text[i + 1] >= 0xdc00 && text[i + 1] <= 0xdfff)
            c = 0x10000 + ((c - 0xd800) << 10) + (text[++i] - 0xdc00);
        bool letter = (c >= U'a' && c <= U'z') || (c >= U'0' && c <= U'9') || c >= 0x80;
        if (letter)
        {
            word.push_back(c);
            result.text.push_back(c);
            result.chars.insert(c);
        }
        else if (!word.empty())
        {
            result.words.insert(word);
            word.clear();
        }
    }
    if (!word.empty())
        result.words.insert(word);
    for (std::size_t length : {2, 3})
        for (std::size_t i = 0; i + length <= result.text.size(); ++i)
            result.grams.insert(result.text.substr(i, length));
    return result;
}
template <class T> inline double overlap(const std::set<T> &a, const std::set<T> &b)
{
    if (a.empty() || b.empty())
        return 0;
    std::size_t intersection = 0;
    auto x = a.begin(), y = b.begin();
    while (x != a.end() && y != b.end())
    {
        if (*x == *y)
        {
            ++intersection;
            ++x;
            ++y;
        }
        else if (*x < *y)
            ++x;
        else
            ++y;
    }
    return double(intersection) / (a.size() + b.size() - intersection);
}
inline double similarity(const Features &a, const Features &b)
{
    if (a.text.empty() || b.text.empty())
        return 0;
    if (a.text == b.text)
        return 1;
    double prefix = a.text.substr(0, (std::min)(std::size_t(5), a.text.size())) ==
                            b.text.substr(0, (std::min)(std::size_t(5), b.text.size()))
                        ? .06
                        : 0;
    return (std::min)(1.0, .46 * overlap(a.words, b.words) + .28 * overlap(a.grams, b.grams) +
                               .2 * overlap(a.chars, b.chars) + prefix);
}
inline Graph buildGraph(const std::vector<Song> &songs, const std::function<bool()> &canceled, int neighbors = 6,
                        double minimum = .035)
{
    Graph graph;
    auto n = songs.size();
    graph.points.resize(n);
    graph.weights.resize(n);
    std::vector<Features> names;
    names.reserve(n);
    for (const auto &song : songs)
        names.push_back(filenameFeatures(song.path));
    std::set<std::pair<std::size_t, std::size_t>> seen;
    for (std::size_t i = 0; i < n; ++i)
    {
        if (canceled())
            return {};
        std::vector<std::pair<double, std::size_t>> candidates;
        // Large collections retain every node; bounded candidate work keeps analysis
        // off the player's critical path. Exact all-pair scores are used up to 1200.
        for (std::size_t k = 0; k < (n <= 1200 ? n : (std::min)(n, std::size_t(384))); ++k)
        {
            std::size_t j = n <= 1200 ? k : (i + k * 37) % n;
            if (i == j)
                continue;
            double score = similarity(names[i], names[j]);
            if (score > minimum)
                candidates.emplace_back(score, j);
        }
        std::sort(candidates.begin(), candidates.end(), std::greater<>());
        for (std::size_t k = 0; k < (std::min)(candidates.size(), std::size_t(std::clamp(neighbors, 2, 8))); ++k)
        {
            auto [strength, j] = candidates[k];
            auto a = (std::min)(i, j), b = (std::max)(i, j);
            if (seen.emplace(a, b).second)
            {
                graph.edges.push_back({a, b, strength});
                graph.weights[a] += strength;
                graph.weights[b] += strength;
            }
        }
    }
    if (!n)
        return graph;
    auto center = std::size_t(std::max_element(graph.weights.begin(), graph.weights.end()) - graph.weights.begin());
    for (std::size_t i = 0; i < n; ++i)
    {
        double angle = i * 2.3999632297, radius = 24 * std::sqrt(double(i + 1));
        graph.points[i] = {std::cos(angle) * radius, std::sin(angle) * radius};
    }
    graph.points[center] = {0, 0};
    std::vector<Point> velocity(n);
    for (int step = 0; step < 120; ++step)
    {
        if (canceled())
            return {};
        std::vector<Point> force(n);
        for (std::size_t i = 0; i < n; ++i)
        {
            force[i] = {-graph.points[i].x * .003, -graph.points[i].y * .003};
            auto count = n < 500 ? n : std::size_t(64);
            for (std::size_t k = 0; k < count; ++k)
            {
                std::size_t j = n < 500 ? k : (i + k * 31) % n;
                if (i == j)
                    continue;
                auto delta = Point{graph.points[i].x - graph.points[j].x, graph.points[i].y - graph.points[j].y};
                double d2 = delta.x * delta.x + delta.y * delta.y + 10;
                force[i].x += delta.x * 38 / d2;
                force[i].y += delta.y * 38 / d2;
            }
        }
        for (auto edge : graph.edges)
        {
            auto delta =
                Point{graph.points[edge.b].x - graph.points[edge.a].x, graph.points[edge.b].y - graph.points[edge.a].y};
            double distance = std::hypot(delta.x, delta.y) + .01, target = 28 + 130 * (1 - edge.strength);
            double spring = (distance - target) * (.008 + .018 * edge.strength) / distance;
            force[edge.a].x += delta.x * spring;
            force[edge.a].y += delta.y * spring;
            force[edge.b].x -= delta.x * spring;
            force[edge.b].y -= delta.y * spring;
        }
        force[center].x -= graph.points[center].x * .08;
        force[center].y -= graph.points[center].y * .08;
        for (std::size_t i = 0; i < n; ++i)
        {
            velocity[i].x = (velocity[i].x + force[i].x) * .75;
            velocity[i].y = (velocity[i].y + force[i].y) * .75;
            graph.points[i].x += velocity[i].x;
            graph.points[i].y += velocity[i].y;
        }
    }
    double max = (std::max)(1.0, *std::max_element(graph.weights.begin(), graph.weights.end()));
    for (auto &w : graph.weights)
        w /= max;
    return graph;
}

inline std::uint64_t graphSignature(const std::vector<Song> &songs)
{
    std::uint64_t hash = 1469598103934665603ULL;
    auto add = [&](const void *data, std::size_t length) {
        auto bytes = static_cast<const unsigned char *>(data);
        for (std::size_t i = 0; i < length; ++i)
        {
            hash ^= bytes[i];
            hash *= 1099511628211ULL;
        }
    };
    for (auto &song : songs)
    {
        add(song.path.data(), song.path.size() * 2);
        add(&song.bytes, sizeof(song.bytes));
        add(&song.modified, sizeof(song.modified));
    }
    return hash;
}
inline bool loadGraphCache(const std::filesystem::path &directory, const std::vector<Song> &songs, Graph &graph,
                           int neighbors = 6, double minimum = .035)
{
    auto file = directory / L"graph-v2.bin";
    std::error_code error;
    auto length = std::filesystem::file_size(file, error);
    if (error || length > 128 * 1024 * 1024)
        return false;
    std::ifstream stream(file, std::ios::binary);
    char magic[8]{};
    std::uint64_t signature = 0;
    std::uint32_t count = 0, edges = 0;
    stream.read(magic, 8);
    stream.read(reinterpret_cast<char *>(&signature), 8);
    stream.read(reinterpret_cast<char *>(&count), 4);
    stream.read(reinterpret_cast<char *>(&edges), 4);
    if (!stream || std::string(magic, 8) != "GAGRPH02" ||
        signature != (graphSignature(songs) ^ std::uint64_t(neighbors * 100000 + int(minimum * 10000))) ||
        count != songs.size() || edges > std::uint64_t(count) * 8)
        return false;
    Graph candidate;
    candidate.points.resize(count);
    candidate.weights.resize(count);
    candidate.edges.resize(edges);
    stream.read(reinterpret_cast<char *>(candidate.points.data()), count * sizeof(Point));
    stream.read(reinterpret_cast<char *>(candidate.weights.data()), count * sizeof(double));
    stream.read(reinterpret_cast<char *>(candidate.edges.data()), edges * sizeof(Edge));
    if (!stream)
        return false;
    for (auto p : candidate.points)
        if (!std::isfinite(p.x) || !std::isfinite(p.y) || std::abs(p.x) > 1e8 || std::abs(p.y) > 1e8)
            return false;
    for (auto w : candidate.weights)
        if (!std::isfinite(w) || w < 0 || w > 1)
            return false;
    for (auto e : candidate.edges)
        if (e.a >= count || e.b >= count || !std::isfinite(e.strength) || e.strength < 0 || e.strength > 1)
            return false;
    graph = std::move(candidate);
    return true;
}
inline bool saveGraphCache(const std::filesystem::path &directory, const std::vector<Song> &songs, const Graph &graph,
                           int neighbors = 6, double minimum = .035)
{
    if (graph.points.size() != songs.size() || graph.weights.size() != songs.size())
        return false;
    std::error_code error;
    std::filesystem::create_directories(directory, error);
    if (error)
        return false;
    auto temp = directory / L"graph-v2.tmp", file = directory / L"graph-v2.bin";
    std::ofstream stream(temp, std::ios::binary | std::ios::trunc);
    auto signature = graphSignature(songs) ^ std::uint64_t(neighbors * 100000 + int(minimum * 10000));
    std::uint32_t count = std::uint32_t(graph.points.size()), edges = std::uint32_t(graph.edges.size());
    stream.write("GAGRPH02", 8);
    stream.write(reinterpret_cast<char *>(&signature), 8);
    stream.write(reinterpret_cast<char *>(&count), 4);
    stream.write(reinterpret_cast<char *>(&edges), 4);
    stream.write(reinterpret_cast<const char *>(graph.points.data()), count * sizeof(Point));
    stream.write(reinterpret_cast<const char *>(graph.weights.data()), count * sizeof(double));
    stream.write(reinterpret_cast<const char *>(graph.edges.data()), edges * sizeof(Edge));
    stream.close();
    return stream && MoveFileEx(temp.c_str(), file.c_str(), MOVEFILE_REPLACE_EXISTING | MOVEFILE_WRITE_THROUGH);
}

inline void writeString(std::ostream &stream, const std::wstring &text)
{
    std::uint32_t length = std::uint32_t(text.size());
    stream.write(reinterpret_cast<char *>(&length), 4);
    stream.write(reinterpret_cast<const char *>(text.data()), length * 2);
}
inline bool readString(std::istream &stream, std::wstring &text)
{
    std::uint32_t length = 0;
    stream.read(reinterpret_cast<char *>(&length), 4);
    if (!stream || length > 32768)
        return false;
    text.resize(length);
    stream.read(reinterpret_cast<char *>(text.data()), length * 2);
    return bool(stream);
}
inline bool saveSettings(const std::filesystem::path &directory, const Settings &settings)
{
    std::error_code error;
    std::filesystem::create_directories(directory, error);
    if (error)
        return false;
    auto temp = directory / L"settings-v1.tmp", file = directory / L"settings-v1.bin";
    std::ofstream stream(temp, std::ios::binary | std::ios::trunc);
    stream.write("GADESK02", 8);
    for (int v : {settings.palette, settings.row, settings.miniSize, settings.sort, int(settings.loop)})
        stream.write(reinterpret_cast<char *>(&v), 4);
    stream.write(reinterpret_cast<const char *>(&settings.leftWidth), 8);
    stream.write(reinterpret_cast<const char *>(&settings.rightWidth), 8);
    std::uint32_t count = std::uint32_t(settings.roots.size());
    stream.write(reinterpret_cast<char *>(&count), 4);
    for (auto &root : settings.roots)
        writeString(stream, root);
    count = std::uint32_t(settings.favorites.size());
    stream.write(reinterpret_cast<char *>(&count), 4);
    for (auto &favorite : settings.favorites)
        writeString(stream, favorite);
    count = std::uint32_t(settings.stacks.size());
    stream.write(reinterpret_cast<char *>(&count), 4);
    for (auto &stack : settings.stacks)
    {
        writeString(stream, stack.name);
        count = std::uint32_t(stack.tracks.size());
        stream.write(reinterpret_cast<char *>(&count), 4);
        for (auto &track : stack.tracks)
        {
            writeString(stream, track.path);
            for (int value : {track.gain, int(track.mute), int(track.solo)})
                stream.write(reinterpret_cast<char *>(&value), 4);
            stream.write(reinterpret_cast<const char *>(&track.offset), 8);
        }
    }
    stream.close();
    if (!stream)
        return false;
    return MoveFileEx(temp.c_str(), file.c_str(), MOVEFILE_REPLACE_EXISTING | MOVEFILE_WRITE_THROUGH) != 0;
}
inline Settings loadSettings(const std::filesystem::path &directory)
{
    Settings settings;
    std::error_code error;
    auto length = std::filesystem::file_size(directory / L"settings-v1.bin", error);
    if (!error && length > 32 * 1024 * 1024)
        return settings;
    std::ifstream stream(directory / L"settings-v1.bin", std::ios::binary);
    if (!stream)
        return settings;
    char magic[8]{};
    stream.read(magic, 8);
    bool v2 = std::string(magic, 8) == "GADESK02";
    if (!v2 && std::string(magic, 8) != "GADESK01")
        return {};
    int values[5]{};
    stream.read(reinterpret_cast<char *>(values), 20);
    settings.palette = std::clamp(values[0], 0, 2);
    settings.row = std::clamp(values[1], 0, 2);
    settings.miniSize = std::clamp(values[2], 0, 2);
    settings.sort = std::clamp(values[3], 0, 2);
    settings.loop = values[4] != 0;
    stream.read(reinterpret_cast<char *>(&settings.leftWidth), 8);
    stream.read(reinterpret_cast<char *>(&settings.rightWidth), 8);
    if (!std::isfinite(settings.leftWidth) || !std::isfinite(settings.rightWidth))
        return {};
    auto count = [&](unsigned limit) {
        std::uint32_t n = 0;
        stream.read(reinterpret_cast<char *>(&n), 4);
        return stream && n <= limit ? int(n) : -1;
    };
    int n = count(128);
    if (n < 0)
        return {};
    for (int i = 0; i < n; ++i)
    {
        std::wstring text;
        if (!readString(stream, text))
            return {};
        settings.roots.push_back(text);
    }
    n = count(100000);
    if (n < 0)
        return {};
    for (int i = 0; i < n; ++i)
    {
        std::wstring text;
        if (!readString(stream, text))
            return {};
        settings.favorites.insert(text);
    }
    n = count(1000);
    if (n < 0)
        return {};
    for (int i = 0; i < n; ++i)
    {
        SavedStack stack;
        if (!readString(stream, stack.name))
            return {};
        int tracks = count(8);
        if (tracks < 1)
            return {};
        for (int j = 0; j < tracks; ++j)
        {
            SavedTrack track;
            if (!readString(stream, track.path))
                return {};
            int options[3]{};
            stream.read(reinterpret_cast<char *>(options), 12);
            if (!stream)
                return {};
            track.gain = std::clamp(options[0], 0, 100);
            track.mute = options[1] != 0;
            track.solo = options[2] != 0;
            if (v2)
            {
                stream.read(reinterpret_cast<char *>(&track.offset), 8);
                if (!stream || !std::isfinite(track.offset) || std::abs(track.offset) > 30)
                    return {};
            }
            stack.tracks.push_back(track);
        }
        settings.stacks.push_back(stack);
    }
    return stream ? settings : Settings{};
}
inline std::wstring formatTime(double seconds)
{
    wchar_t text[48]{};
    auto s = std::int64_t((std::max)(0.0, seconds));
    if (s >= 3600)
        swprintf_s(text, L"%lld:%02lld:%02lld", s / 3600, s / 60 % 60, s % 60);
    else
        swprintf_s(text, L"%lld:%02lld", s / 60, s % 60);
    return text;
}
inline int mixedGain(int master, int gain, bool mute, bool solo, bool anySolo)
{
    return mute || (anySolo && !solo) ? 0 : std::clamp(master, 0, 100) * std::clamp(gain, 0, 100) / 100;
}
} // namespace ga
