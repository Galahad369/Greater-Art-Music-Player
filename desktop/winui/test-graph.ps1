param([string]$Dll = (Join-Path $PSScriptRoot 'native-build/GreaterArt.Engine.dll'))

$ErrorActionPreference = 'Stop'
$enginePath = [IO.Path]::GetFullPath($Dll)
if (-not (Test-Path -LiteralPath $enginePath -PathType Leaf)) {
    throw ('Build the native engine before running the graph ABI tests: ' + $enginePath)
}
if ([IO.Path]::GetFileName($enginePath) -ne 'GreaterArt.Engine.dll') {
    throw 'The DLL parameter must point to GreaterArt.Engine.dll beside its approved codec runtime DLLs.'
}

# No media is opened, no audio engine is initialized, and no HWND is created.
# These synthetic paths need not exist; the production graph compares filenames.
if (-not ('GreaterArtGraphAbiProbe' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Diagnostics;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;

public static class GreaterArtGraphAbiProbe
{
    [StructLayout(LayoutKind.Sequential)]
    public struct Point { public double X, Y; }
    [StructLayout(LayoutKind.Sequential)]
    public struct Edge { public int A, B; public double Strength; }
    public sealed class Summary
    {
        public string Status = "PASS";
        public int Assertions, Nodes, Edges;
        public double PartialLatinScore, IdenticalStemScore, RelatedCjkScore;
        public bool WorkerFrames, ExactPin, PreservedGeometry, SettleAndReheat, InvalidInputs;
    }

    private static int assertions;
    [DllImport("kernel32", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern bool SetDllDirectory(string path);
    [DllImport("kernel32", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern uint GetDllDirectory(uint capacity, StringBuilder path);
    [DllImport("GreaterArt.Engine.dll", CallingConvention = CallingConvention.Cdecl)]
    private static extern IntPtr GA_GraphCreate(
        [In, MarshalAs(UnmanagedType.LPArray, ArraySubType = UnmanagedType.LPWStr, SizeParamIndex = 1)] string[] paths,
        int count);
    [DllImport("GreaterArt.Engine.dll", CallingConvention = CallingConvention.Cdecl)]
    private static extern void GA_GraphRelease(IntPtr graph);
    [DllImport("GreaterArt.Engine.dll", CallingConvention = CallingConvention.Cdecl)]
    private static extern int GA_GraphNodeCount(IntPtr graph);
    [DllImport("GreaterArt.Engine.dll", CallingConvention = CallingConvention.Cdecl)]
    private static extern int GA_GraphEdgeCount(IntPtr graph);
    [DllImport("GreaterArt.Engine.dll", CallingConvention = CallingConvention.Cdecl)]
    private static extern int GA_GraphCopyEdges(IntPtr graph, [Out] Edge[] output, int capacity);
    [DllImport("GreaterArt.Engine.dll", CallingConvention = CallingConvention.Cdecl)]
    private static extern int GA_GraphCopyWeights(IntPtr graph, [Out] double[] output, int capacity);
    [DllImport("GreaterArt.Engine.dll", CallingConvention = CallingConvention.Cdecl)]
    private static extern int GA_GraphReset(IntPtr graph, int center, int repel, int link, int distance, int settle);
    [DllImport("GreaterArt.Engine.dll", CallingConvention = CallingConvention.Cdecl)]
    private static extern int GA_GraphRequest(IntPtr graph, double seconds, int pin, double x, double y);
    [DllImport("GreaterArt.Engine.dll", CallingConvention = CallingConvention.Cdecl)]
    private static extern int GA_GraphTake(IntPtr graph, [Out] Point[] output, int capacity, out int awake);

    private static void Require(bool value, string message)
    {
        if (!value) throw new InvalidOperationException("Graph ABI regression: " + message);
        ++assertions;
    }
    private static bool Finite(double value)
    {
        return !double.IsNaN(value) && !double.IsInfinity(value);
    }
    private static Point[] Frame(IntPtr graph, int count, out int awake)
    {
        var points = new Point[count];
        var timer = Stopwatch.StartNew();
        while (timer.ElapsedMilliseconds < 5000)
        {
            int received = GA_GraphTake(graph, points, count, out awake);
            if (received == count && count > 0)
            {
                Require(points.All(p => Finite(p.X) && Finite(p.Y)), "every worker position is finite");
                return points;
            }
            if (received < 0) throw new InvalidOperationException("Graph take failed: " + received);
            Thread.Sleep(1);
        }
        throw new TimeoutException("The native graph worker did not publish a complete frame within 5 seconds.");
    }
    private static void Request(IntPtr graph, double dt, int pin, double x, double y)
    {
        var timer = Stopwatch.StartNew();
        while (timer.ElapsedMilliseconds < 2000)
        {
            int result = GA_GraphRequest(graph, dt, pin, x, y);
            if (result == 1) return;
            if (result < 0) throw new InvalidOperationException("A valid graph request was rejected.");
            Thread.Sleep(1);
        }
        throw new TimeoutException("The native graph worker remained busy for 2 seconds.");
    }

    public static Summary Run(string directory)
    {
        assertions = 0;
        var previousDirectory = new StringBuilder(32768);
        uint previousLength = GetDllDirectory((uint)previousDirectory.Capacity, previousDirectory);
        if (previousLength >= previousDirectory.Capacity)
            throw new InvalidOperationException("The current DLL search directory exceeds the supported path length.");
        if (!SetDllDirectory(directory)) throw new Win32Exception(Marshal.GetLastWin32Error());
        IntPtr graph = IntPtr.Zero, empty = IntPtr.Zero;
        try
        {
            Require(Marshal.SizeOf(typeof(Point)) == 16 && Marshal.SizeOf(typeof(Edge)) == 16,
                    "point and edge ABI layouts are 16 bytes");
            Require(GA_GraphCreate(null, -1) == IntPtr.Zero, "negative count is rejected");
            Require(GA_GraphCreate(null, 100001) == IntPtr.Zero, "excessive count is rejected");
            Require(GA_GraphCreate(null, 1) == IntPtr.Zero, "null path array is rejected");
            Require(GA_GraphCreate(new string[] { null }, 1) == IntPtr.Zero, "null filename is rejected");
            var names = new string[]
            {
                @"C:\GreaterArtGraphFixture\Caravan.mp3",
                @"C:\GreaterArtGraphFixture\Gambling.mp4",
                "C:\\GreaterArtGraphFixture\\\u5b64\u72ec\u6bd2\u6bd2 \u5409\u4e43 cover.mp4",
                "C:\\GreaterArtGraphFixture\\\u5b64\u72ec\u6bd2\u6bd2 \u8389\u72ac.mp4",
                @"C:\GreaterArtGraphFixture\Caravan.mp4"
            };
            graph = GA_GraphCreate(names, names.Length);
            Require(graph != IntPtr.Zero, "Unicode filename graph is created");
            Require(GA_GraphNodeCount(graph) == names.Length, "all library nodes are retained");
            int edgeCount = GA_GraphEdgeCount(graph);
            Require(edgeCount > 0 && edgeCount <= names.Length * 6, "graph connections remain sparse");
            var edges = new Edge[edgeCount];
            var weights = new double[names.Length];
            Require(GA_GraphCopyEdges(graph, edges, edges.Length) == edges.Length, "complete edges are copied");
            Require(GA_GraphCopyWeights(graph, weights, weights.Length) == weights.Length, "complete weights are copied");
            Require(GA_GraphCopyEdges(graph, new Edge[edgeCount - 1], edgeCount - 1) == -2,
                    "insufficient edge capacity is rejected");
            Require(GA_GraphCopyWeights(graph, new double[names.Length - 1], names.Length - 1) == -2,
                    "insufficient weight capacity is rejected");
            Require(GA_GraphCopyEdges(graph, null, edgeCount) == -1, "null edge output is rejected");
            Require(GA_GraphCopyWeights(graph, null, names.Length) == -1, "null weight output is rejected");

            double partial = edges.First(e => e.A == 0 && e.B == 1).Strength;
            double identical = edges.First(e => e.A == 0 && e.B == 4).Strength;
            double cjk = edges.First(e => e.A == 2 && e.B == 3).Strength;
            Require(partial > 0 && partial < identical, "shared Latin letters receive partial credit");
            Require(identical == 1, "matching stems ignore their different media extensions");
            Require(cjk > partial && cjk < identical, "UTF-16 CJK names retain meaningful similarity");
            Require(weights.All(w => Finite(w) && w >= 0 && w <= 1) && weights.Max() == 1,
                    "weighted node sizes are normalized");
            var seen = new HashSet<long>();
            Require(edges.All(e => e.A >= 0 && e.B < names.Length && e.A < e.B &&
                        Finite(e.Strength) && e.Strength > 0 && e.Strength <= 1 &&
                        seen.Add(((long)e.A << 32) | (uint)e.B)), "edges have valid unique endpoint pairs");

            int awake;
            Require(GA_GraphTake(graph, new Point[names.Length - 1], names.Length - 1, out awake) == -2,
                    "insufficient point capacity is rejected without consuming the frame");
            Require(GA_GraphTake(graph, null, names.Length, out awake) == -1, "null point output is rejected");
            Frame(graph, names.Length, out awake);
            Require(awake == 1, "a multi-node graph begins awake");
            Require(GA_GraphRequest(graph, double.NaN, -1, 0, 0) == -1, "NaN elapsed time is rejected");
            Require(GA_GraphRequest(graph, -.01, -1, 0, 0) == -1, "negative elapsed time is rejected");
            Require(GA_GraphRequest(graph, .01, names.Length, 0, 0) == -1, "out-of-range pin is rejected");
            Require(GA_GraphRequest(graph, .01, 0, double.PositiveInfinity, 0) == -1,
                    "non-finite cursor position is rejected");
            Request(graph, 1.0 / 60, 0, 320, -120);
            var pinned = Frame(graph, names.Length, out awake);
            Require(pinned[0].X == 320 && pinned[0].Y == -120, "a dragged node stays exactly at the cursor");

            Require(GA_GraphReset(graph, 100, 100, 100, 100, 0) == 0, "force reset succeeds");
            var reset = Frame(graph, names.Length, out awake);
            Require(reset.Zip(pinned, (a, b) => Math.Abs(a.X - b.X) + Math.Abs(a.Y - b.Y)).Max() < 1e-10,
                    "force changes retain the latest displayed geometry");
            Require(awake == 1, "reset reheats the graph");
            Require(GA_GraphReset(graph, 100, 100, 100, 100, 1) == 0, "settle is accepted");
            Frame(graph, names.Length, out awake);
            Require(awake == 0, "the background worker settles without a UI simulation loop");
            Request(graph, 0, 1, -81, 44);
            var reheated = Frame(graph, names.Length, out awake);
            Require(reheated[1].X == -81 && reheated[1].Y == 44 && awake == 1,
                    "dragging reheats a settled graph");
            Request(graph, 1.0 / 60, -1, 0, 0);
            Frame(graph, names.Length, out awake);

            GA_GraphRelease(graph);
            Require(GA_GraphNodeCount(graph) == -1 && GA_GraphEdgeCount(graph) == -1,
                    "released handles are rejected");
            Require(GA_GraphRequest(graph, .01, -1, 0, 0) == -1, "a released worker cannot receive requests");
            Require(GA_GraphReset(graph, 100, 100, 100, 100, 0) == unchecked((int)0x80070006),
                    "reset returns E_HANDLE after release");
            GA_GraphRelease(graph); // Idempotent release must not crash.
            graph = IntPtr.Zero;
            empty = GA_GraphCreate(new string[0], 0);
            Require(empty != IntPtr.Zero && GA_GraphNodeCount(empty) == 0 && GA_GraphEdgeCount(empty) == 0,
                    "an empty library is handled gracefully");
            return new Summary
            {
                Assertions = assertions, Nodes = names.Length, Edges = edgeCount,
                PartialLatinScore = partial, IdenticalStemScore = identical, RelatedCjkScore = cjk,
                WorkerFrames = true, ExactPin = true, PreservedGeometry = true,
                SettleAndReheat = true, InvalidInputs = true
            };
        }
        finally
        {
            if (graph != IntPtr.Zero) GA_GraphRelease(graph);
            if (empty != IntPtr.Zero) GA_GraphRelease(empty);
            SetDllDirectory(previousLength == 0 ? null : previousDirectory.ToString());
        }
    }
}
'@
}

$summary = [GreaterArtGraphAbiProbe]::Run([IO.Path]::GetDirectoryName($enginePath))
$summary | ConvertTo-Json
Write-Output ('PASS: native graph ABI (' + $summary.Assertions + ' assertions; no UI, audio, or real media).')
