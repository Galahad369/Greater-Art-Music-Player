using System;
using System.Collections.Generic;
using System.Text;

namespace GreaterArt.Desktop;

internal readonly record struct ExplorationEdge(int A, int B, double Strength);

internal sealed record GraphColorGroup(string Query, uint Argb);

internal sealed record GraphExplorationResult(
    bool[] Visible,
    int[] EdgeIndices,
    int[] Degrees,
    double[] Weights,
    uint?[] GroupColors,
    int VisibleCount);

// Compute on a filter/group/selection change, then reuse the snapshot while drawing.
// Queries are filename substrings, not Obsidian's metadata or backlink query language.
internal sealed class GraphExploration
{
    private readonly string[] titles;
    private readonly ExplorationEdge[] edges;
    private readonly List<int>[] adjacency;

    public GraphExploration(string[] titles, ExplorationEdge[] edges)
    {
        ArgumentNullException.ThrowIfNull(titles);
        ArgumentNullException.ThrowIfNull(edges);
        this.titles = new string[titles.Length];
        this.edges = (ExplorationEdge[])edges.Clone();
        adjacency = new List<int>[titles.Length];
        for (int i = 0; i < titles.Length; i++)
        {
            if (titles[i] is null)
                throw new ArgumentException("Every graph node needs a filename.", nameof(titles));
            this.titles[i] = titles[i].Normalize(NormalizationForm.FormC);
            adjacency[i] = new List<int>();
        }
        for (int i = 0; i < this.edges.Length; i++)
        {
            var edge = this.edges[i];
            if (edge.A < 0 || edge.A >= titles.Length || edge.B < 0 || edge.B >= titles.Length ||
                !double.IsFinite(edge.Strength) || edge.Strength < 0 || edge.Strength > 1)
                throw new ArgumentException("A graph connection has an invalid endpoint or strength.", nameof(edges));
            adjacency[edge.A].Add(i);
            if (edge.B != edge.A) adjacency[edge.B].Add(i);
        }
    }

    public GraphExplorationResult Compute(
        string query = "",
        bool showOrphans = true,
        double minimumStrength = 0,
        int? localCenter = null,
        int localDepth = 1,
        IReadOnlyList<GraphColorGroup>? groups = null)
    {
        ArgumentNullException.ThrowIfNull(query);
        if (!double.IsFinite(minimumStrength) || minimumStrength < 0 || minimumStrength > 1)
            throw new ArgumentOutOfRangeException(nameof(minimumStrength));
        if (localDepth < 1 || localDepth > 3)
            throw new ArgumentOutOfRangeException(nameof(localDepth));
        if (localCenter is int center && (center < 0 || center >= titles.Length))
            throw new ArgumentOutOfRangeException(nameof(localCenter));

        var tokens = Tokens(query);
        var visible = new bool[titles.Length];
        for (int i = 0; i < titles.Length; i++) visible[i] = Matches(titles[i], tokens);

        var eligibleEdges = new bool[edges.Length];
        for (int i = 0; i < edges.Length; i++)
        {
            var edge = edges[i];
            eligibleEdges[i] = edge.Strength >= minimumStrength && visible[edge.A] && visible[edge.B];
        }

        // Search within the filtered graph: hidden nodes and weak links do not bridge
        // otherwise disconnected local neighborhoods. The center also obeys the query.
        if (localCenter is int selected)
        {
            var reached = new bool[titles.Length];
            var queue = new Queue<(int Node, int Depth)>();
            if (visible[selected])
            {
                reached[selected] = true;
                queue.Enqueue((selected, 0));
            }
            while (queue.TryDequeue(out var item))
            {
                if (item.Depth == localDepth) continue;
                foreach (int edgeIndex in adjacency[item.Node])
                {
                    if (!eligibleEdges[edgeIndex]) continue;
                    var edge = edges[edgeIndex];
                    int neighbor = edge.A == item.Node ? edge.B : edge.A;
                    if (reached[neighbor]) continue;
                    reached[neighbor] = true;
                    queue.Enqueue((neighbor, item.Depth + 1));
                }
            }
            visible = reached;
        }

        var edgeIndices = new List<int>();
        var degrees = new int[titles.Length];
        var weights = new double[titles.Length];
        for (int i = 0; i < edges.Length; i++)
        {
            var edge = edges[i];
            if (!eligibleEdges[i] || !visible[edge.A] || !visible[edge.B]) continue;
            edgeIndices.Add(i);
            degrees[edge.A]++;
            degrees[edge.B]++;
            weights[edge.A] += edge.Strength;
            weights[edge.B] += edge.Strength;
        }

        var groupTokens = new string[groups?.Count ?? 0][];
        for (int i = 0; i < groupTokens.Length; i++) groupTokens[i] = Tokens(groups![i].Query);
        var groupColors = new uint?[titles.Length];
        int visibleCount = 0;
        double maximumWeight = 0;
        for (int i = 0; i < titles.Length; i++)
        {
            if (!showOrphans && degrees[i] == 0) visible[i] = false;
            if (!visible[i]) continue;
            visibleCount++;
            maximumWeight = Math.Max(maximumWeight, weights[i]);
            for (int group = 0; group < groupTokens.Length; group++)
            {
                // An unfinished blank group must not override every later group.
                if (groupTokens[group].Length == 0 || !Matches(titles[i], groupTokens[group])) continue;
                groupColors[i] = groups![group].Argb;
                break;
            }
        }
        if (maximumWeight > 0)
            for (int i = 0; i < weights.Length; i++) weights[i] /= maximumWeight;
        return new GraphExplorationResult(visible, edgeIndices.ToArray(), degrees, weights, groupColors, visibleCount);
    }

    private static string[] Tokens(string query) =>
        query.Normalize(NormalizationForm.FormC).Split((char[]?)null, StringSplitOptions.RemoveEmptyEntries);

    private static bool Matches(string title, string[] tokens)
    {
        foreach (string token in tokens)
            if (!title.Contains(token, StringComparison.OrdinalIgnoreCase)) return false;
        return true;
    }
}
