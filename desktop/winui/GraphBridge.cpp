#define NOMINMAX
#include "GraphBridge.h"
#include "../native/GraphMotion.h"
#include <cwchar>
#include <memory>
#include <mutex>
#include <unordered_map>

namespace
{
constexpr int maxNodes = 100000;
constexpr std::size_t maxPathLength = 32768;
struct GraphBridge
{
    ga::Graph graph;
    ga::GraphMotion motion;
    std::atomic<unsigned> generation{0};
    std::mutex stateMutex;
};
std::mutex graphsMutex;
std::unordered_map<void *, std::shared_ptr<GraphBridge>> graphs;

std::shared_ptr<GraphBridge> findGraph(void *handle)
{
    std::lock_guard lock(graphsMutex);
    const auto found = graphs.find(handle);
    return found == graphs.end() ? nullptr : found->second;
}
template <class Function> int graphResult(Function action) noexcept
{
    try { return action(); }
    catch (...) { return -1; }
}
}

static_assert(sizeof(GA_GraphPoint) == 16);
static_assert(sizeof(GA_GraphEdge) == 16);

void *GA_GraphCreate(const wchar_t *const *paths, int count) noexcept
{
    if (count < 0 || count > maxNodes || (count && !paths)) return nullptr;
    try
    {
        std::vector<ga::Song> songs;
        songs.reserve(count);
        for (int i = 0; i < count; ++i)
        {
            if (!paths[i]) return nullptr;
            const auto length = wcsnlen_s(paths[i], maxPathLength);
            if (!length || length >= maxPathLength) return nullptr;
            ga::Song song;
            song.path.assign(paths[i], length);
            songs.push_back(std::move(song));
        }
        auto bridge = std::make_shared<GraphBridge>();
        // Reuse the desktop's Unicode filename scoring, sparse edges and exact
        // Barnes-Hut worker. No second layout algorithm runs in the WinUI layer.
        bridge->graph = ga::buildGraph(songs, [] { return false; });
        bridge->motion.start();
        bridge->generation = bridge->motion.reset(bridge->graph, {});
        auto handle = bridge.get();
        std::lock_guard lock(graphsMutex);
        graphs.emplace(handle, std::move(bridge));
        return handle;
    }
    catch (...) { return nullptr; }
}
void GA_GraphRelease(void *handle) noexcept
{
    try
    {
        std::shared_ptr<GraphBridge> released;
        {
            std::lock_guard lock(graphsMutex);
            const auto found = graphs.find(handle);
            if (found == graphs.end()) return;
            released = std::move(found->second);
            graphs.erase(found);
        }
        // Joining the physics worker never holds the registry mutex. An API
        // call already in flight retains ownership until its copy has completed.
    }
    catch (...) { }
}
int GA_GraphNodeCount(void *handle) noexcept
{
    return graphResult([&] {
        auto bridge = findGraph(handle);
        return bridge ? static_cast<int>(bridge->graph.points.size()) : -1;
    });
}
int GA_GraphEdgeCount(void *handle) noexcept
{
    return graphResult([&] {
        auto bridge = findGraph(handle);
        return bridge ? static_cast<int>(bridge->graph.edges.size()) : -1;
    });
}
int GA_GraphCopyEdges(void *handle, GA_GraphEdge *edges, int capacity) noexcept
{
    return graphResult([&] {
        auto bridge = findGraph(handle);
        if (!bridge || capacity < 0) return -1;
        const auto count = static_cast<int>(bridge->graph.edges.size());
        if (capacity < count) return -2;
        if (count && !edges) return -1;
        for (int i = 0; i < count; ++i)
        {
            const auto &edge = bridge->graph.edges[i];
            edges[i] = {static_cast<int>(edge.a), static_cast<int>(edge.b), edge.strength};
        }
        return count;
    });
}
int GA_GraphCopyWeights(void *handle, double *weights, int capacity) noexcept
{
    return graphResult([&] {
        auto bridge = findGraph(handle);
        if (!bridge || capacity < 0) return -1;
        const auto count = static_cast<int>(bridge->graph.weights.size());
        if (capacity < count) return -2;
        if (count && !weights) return -1;
        std::copy(bridge->graph.weights.begin(), bridge->graph.weights.end(), weights);
        return count;
    });
}
HRESULT GA_GraphReset(void *handle, int center, int repel, int link, int distance, int settle) noexcept
{
    try
    {
        auto bridge = findGraph(handle);
        if (!bridge) return E_HANDLE;
        ga::GraphForces forces{std::clamp(center, 25, 200), std::clamp(repel, 25, 200),
                               std::clamp(link, 25, 200), std::clamp(distance, 25, 200)};
        std::lock_guard lock(bridge->stateMutex);
        bridge->generation = bridge->motion.reset(bridge->graph, forces, settle != 0);
        return S_OK;
    }
    catch (const std::bad_alloc &) { return E_OUTOFMEMORY; }
    catch (...) { return E_FAIL; }
}
int GA_GraphRequest(void *handle, double dt, int pin, double x, double y) noexcept
{
    return graphResult([&] {
        auto bridge = findGraph(handle);
        if (!bridge || !std::isfinite(dt) || dt < 0 || !std::isfinite(x) || !std::isfinite(y) ||
            pin < -1 || pin >= static_cast<int>(bridge->graph.points.size())) return -1;
        return bridge->motion.request(dt, pin, {x, y}) ? 1 : 0;
    });
}
int GA_GraphTake(void *handle, GA_GraphPoint *points, int capacity, int *awake) noexcept
{
    return graphResult([&] {
        if (!awake) return -1;
        *awake = 0;
        auto bridge = findGraph(handle);
        if (!bridge || capacity < 0) return -1;
        const auto count = static_cast<int>(bridge->graph.points.size());
        if (capacity < count) return -2;
        if (count && !points) return -1;
        std::lock_guard lock(bridge->stateMutex);
        auto frame = bridge->motion.take();
        if (!frame || frame->generation != bridge->generation.load()) return 0;
        if (frame->points.size() != bridge->graph.points.size()) return -1;
        for (int i = 0; i < count; ++i)
        {
            points[i] = {frame->points[i].x, frame->points[i].y};
            // Match the original desktop: changing force settings reheats the
            // displayed geometry, rather than jumping to its original seed.
            bridge->graph.points[i] = frame->points[i];
        }
        *awake = frame->awake ? 1 : 0;
        return count;
    });
}
