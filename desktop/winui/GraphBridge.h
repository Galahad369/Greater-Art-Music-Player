#pragma once
#include <cstdint>
#include <windows.h>

struct GA_GraphPoint
{
    double x, y;
};
struct GA_GraphEdge
{
    std::int32_t a, b;
    double strength;
};

#define GA_GRAPH_EXPORT extern "C" __declspec(dllexport)
// Create does filename analysis and must be called from a background task.
GA_GRAPH_EXPORT void *GA_GraphCreate(const wchar_t *const *paths, int count) noexcept;
GA_GRAPH_EXPORT void GA_GraphRelease(void *graph) noexcept;
GA_GRAPH_EXPORT int GA_GraphNodeCount(void *graph) noexcept;
GA_GRAPH_EXPORT int GA_GraphEdgeCount(void *graph) noexcept;
// Array APIs return the complete count, -1 for invalid input, -2 for insufficient
// capacity. They never truncate the graph or retain caller-owned arrays.
GA_GRAPH_EXPORT int GA_GraphCopyEdges(void *graph, GA_GraphEdge *edges, int capacity) noexcept;
GA_GRAPH_EXPORT int GA_GraphCopyWeights(void *graph, double *weights, int capacity) noexcept;
GA_GRAPH_EXPORT HRESULT GA_GraphReset(void *graph, int center, int repel, int link, int distance, int settle) noexcept;
// Request is nonblocking: 1 accepted, 0 worker mutex busy, -1 invalid input.
GA_GRAPH_EXPORT int GA_GraphRequest(void *graph, double dt, int pin, double x, double y) noexcept;
// Take returns 0 when there is no new frame; otherwise the complete point count.
GA_GRAPH_EXPORT int GA_GraphTake(void *graph, GA_GraphPoint *points, int capacity, int *awake) noexcept;
