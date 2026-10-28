using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Threading;

namespace GreaterArt.Desktop;

[StructLayout(LayoutKind.Sequential)]
internal struct GraphPoint
{
    public double X, Y;
    public GraphPoint(double x, double y) { X = x; Y = y; }
}

[StructLayout(LayoutKind.Sequential)]
internal struct GraphEdge
{
    public int A, B;
    public double Strength;
}

// Build and physics stay in the existing native graph implementation. The UI
// consumes snapshots; it never advances the simulation or rebuilds similarity.
internal sealed class GraphEngine : IDisposable
{
    private nint handle;
    public int Count { get; }
    public GraphEdge[] Edges { get; }
    public double[] Weights { get; }

    private GraphEngine(nint handle, int count, GraphEdge[] edges, double[] weights)
    { this.handle = handle; Count = count; Edges = edges; Weights = weights; }

    public static GraphEngine Build(string[] paths)
    {
        nint graph = GA_GraphCreate(paths, paths.Length);
        if (graph == 0) throw new InvalidOperationException("The filename graph could not be built.");
        try
        {
            int count = GA_GraphNodeCount(graph), edgeCount = GA_GraphEdgeCount(graph);
            if (count != paths.Length || edgeCount < 0)
                throw new InvalidOperationException("The graph did not preserve the complete library.");
            var edges = new GraphEdge[edgeCount];
            var weights = new double[count];
            if (GA_GraphCopyEdges(graph, edges, edges.Length) != edgeCount ||
                GA_GraphCopyWeights(graph, weights, weights.Length) != count)
                throw new InvalidOperationException("The graph snapshot was incomplete.");
            foreach (var edge in edges)
                if (edge.A < 0 || edge.A >= count || edge.B < 0 || edge.B >= count ||
                    !double.IsFinite(edge.Strength) || edge.Strength < 0 || edge.Strength > 1)
                    throw new InvalidOperationException("The graph returned an invalid connection.");
            foreach (double weight in weights)
                if (!double.IsFinite(weight) || weight < 0 || weight > 1)
                    throw new InvalidOperationException("The graph returned an invalid node weight.");
            return new GraphEngine(graph, count, edges, weights);
        }
        catch { GA_GraphRelease(graph); throw; }
    }

    public void Reset(int center, int repel, int link, int distance, bool settle = false) =>
        Engine.Check(GA_GraphReset(handle, center, repel, link, distance, settle ? 1 : 0));

    public void Request(double seconds, int pin, GraphPoint point)
    {
        if (GA_GraphRequest(handle, seconds, pin, point.X, point.Y) < 0)
            throw new InvalidOperationException("The graph worker is unavailable.");
    }

    public bool Take(GraphPoint[] points, out bool awake)
    {
        int count = GA_GraphTake(handle, points, points.Length, out int moving);
        awake = moving != 0;
        if (count == 0) return false;
        if (count != Count) throw new InvalidOperationException("The graph worker returned an incomplete frame.");
        foreach (var point in points)
            if (!double.IsFinite(point.X) || !double.IsFinite(point.Y))
                throw new InvalidOperationException("The graph worker returned a non-finite position.");
        return true;
    }

    public void Dispose()
    {
        nint previous = Interlocked.Exchange(ref handle, 0);
        if (previous != 0) GA_GraphRelease(previous);
    }

    private const string Library = "GreaterArt.Engine.dll";
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl, CharSet = CharSet.Unicode)]
    private static extern nint GA_GraphCreate([MarshalAs(UnmanagedType.LPArray, ArraySubType = UnmanagedType.LPWStr, SizeParamIndex = 1)] string[] paths, int count);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] private static extern void GA_GraphRelease(nint graph);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] private static extern int GA_GraphNodeCount(nint graph);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] private static extern int GA_GraphEdgeCount(nint graph);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] private static extern int GA_GraphCopyEdges(nint graph, [Out] GraphEdge[] output, int capacity);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] private static extern int GA_GraphCopyWeights(nint graph, [Out] double[] output, int capacity);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] private static extern int GA_GraphReset(nint graph, int center, int repel, int link, int distance, int settle);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] private static extern int GA_GraphRequest(nint graph, double seconds, int pin, double x, double y);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] private static extern int GA_GraphTake(nint graph, [Out] GraphPoint[] output, int capacity, out int awake);
}

internal sealed class GraphViewport
{
    public double Scale { get; private set; } = 1;
    public double PanX { get; private set; }
    public double PanY { get; private set; }
    public GraphPoint Screen(GraphPoint point, double width, double height) =>
        new(point.X * Scale + width / 2 + PanX, point.Y * Scale + height / 2 + PanY);
    public GraphPoint World(GraphPoint point, double width, double height) =>
        new((point.X - width / 2 - PanX) / Scale, (point.Y - height / 2 - PanY) / Scale);
    public void Pan(double x, double y) { PanX += x; PanY += y; }
    public void ZoomAt(GraphPoint screen, double factor, double width, double height)
    {
        if (!double.IsFinite(factor) || factor <= 0) return;
        var anchor = World(screen, width, height);
        Scale = Math.Clamp(Scale * factor, .001, 12);
        PanX = screen.X - width / 2 - anchor.X * Scale;
        PanY = screen.Y - height / 2 - anchor.Y * Scale;
    }
    public void Center(GraphPoint point) { PanX = -point.X * Scale; PanY = -point.Y * Scale; }
    public void Fit(GraphPoint[] points, double width, double height)
    {
        if (points.Length == 0 || width < 1 || height < 1) return;
        double left = double.PositiveInfinity, top = left, right = double.NegativeInfinity, bottom = right;
        foreach (var point in points)
        { left = Math.Min(left, point.X - 22); top = Math.Min(top, point.Y - 22); right = Math.Max(right, point.X + 22); bottom = Math.Max(bottom, point.Y + 22); }
        Scale = Math.Clamp(Math.Min(Math.Max(1, width - 64) / Math.Max(1, right - left), Math.Max(1, height - 64) / Math.Max(1, bottom - top)), .001, 4);
        Center(new GraphPoint((left + right) / 2, (top + bottom) / 2));
    }

    public static IReadOnlyDictionary<string, bool> Verify()
    {
        const double width = 960, height = 600;
        var view = new GraphViewport();
        var cursor = new GraphPoint(317, 211);
        var anchor = view.World(cursor, width, height);
        view.ZoomAt(cursor, 2.3, width, height);
        var same = view.World(cursor, width, height);
        bool anchored = Math.Abs(anchor.X - same.X) < 1e-8 && Math.Abs(anchor.Y - same.Y) < 1e-8;
        view.Pan(83, -29);
        var world = new GraphPoint(-137, 94);
        var roundtrip = view.World(view.Screen(world, width, height), width, height);
        var drag = view.Screen(view.World(cursor, width, height), width, height);
        var points = new[] { new GraphPoint(-410, -211), new GraphPoint(539, 317), new GraphPoint(0, 0) };
        view.Fit(points, width, height);
        bool fitted = true;
        foreach (var point in points)
        { var screen = view.Screen(point, width, height); double radius = 22 * view.Scale; fitted &= screen.X - radius >= 0 && screen.X + radius <= width && screen.Y - radius >= 0 && screen.Y + radius <= height; }
        return new Dictionary<string, bool>
        {
            ["cursor-anchored zoom"] = anchored,
            ["pan/zoom inverse"] = Math.Abs(world.X - roundtrip.X) < 1e-8 && Math.Abs(world.Y - roundtrip.Y) < 1e-8,
            ["drag remains at cursor"] = Math.Abs(cursor.X - drag.X) < 1e-8 && Math.Abs(cursor.Y - drag.Y) < 1e-8,
            ["fit includes every node"] = fitted,
            ["finite geometry transform"] = double.IsFinite(view.Scale) && double.IsFinite(view.PanX) && double.IsFinite(view.PanY),
            ["ABI point and edge sizes"] = Marshal.SizeOf<GraphPoint>() == 16 && Marshal.SizeOf<GraphEdge>() == 16,
        };
    }
}
