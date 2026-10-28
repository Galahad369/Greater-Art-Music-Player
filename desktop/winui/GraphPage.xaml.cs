using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Windows.Foundation;
using Windows.UI.ViewManagement;

namespace GreaterArt.Desktop;

public sealed partial class GraphPage : UserControl, IDisposable
{
    private LibraryTrack[] tracks = Array.Empty<LibraryTrack>();
    private GraphEngine? engine;
    private GraphPoint[] points = Array.Empty<GraphPoint>();
    private EllipseGeometry[] circles = Array.Empty<EllipseGeometry>();
    private (PathFigure Figure, LineSegment End)[] segments = Array.Empty<(PathFigure, LineSegment)>();
    private readonly List<(int Index, PathFigure Figure, LineSegment End)> hoverSegments = new();
    private readonly GraphViewport view = new();
    private readonly Stopwatch clock = new();
    private readonly Stopwatch reveal = new();
    private readonly UISettings systemUi = new();
    private readonly List<MatrixTransform> transforms = new();
    private readonly EllipseGeometry playing = new();
    private bool active, disposed, building, rendering, awake = true, needsFit, hasFrame, dragging, moved, pinching, revealing, changingForces;
    private int revision, pin = -1, hover = -1;
    private GraphPoint pointer, pressed, pinned, grabOffset;
    private double lastTick;

    public event Action<LibraryTrack>? TrackRequested;
    public int NodeCount => engine?.Count ?? 0;
    public int EdgeCount => engine?.Edges.Length ?? 0;
    public bool IsReady => engine != null && points.Length == engine.Count && hasFrame && !needsFit;
    public bool IsMotionActive => rendering;
    public bool HasFiniteGeometry => IsReady && points.All(point => double.IsFinite(point.X) && double.IsFinite(point.Y));
    public long RenderCallbackCount { get; private set; }
    public long SnapshotCount { get; private set; }
    public double RenderTotalMilliseconds { get; private set; }
    public double RenderWorstMilliseconds { get; private set; }
    public double RenderAverageMilliseconds => RenderCallbackCount == 0 ? 0 : RenderTotalMilliseconds / RenderCallbackCount;
    public static IReadOnlyDictionary<string, bool> Verify() => GraphViewport.Verify();

    public GraphPage()
    {
        InitializeComponent();
        foreach (var path in new[] { SoftEdges, MediumEdges, StrongEdges, HoverEdges, QuietNodes, MiddleNodes, HubNodes, PlayingRing })
        {
            var transform = new MatrixTransform();
            path.RenderTransform = transform;
            transforms.Add(transform);
        }
        PlayingRing.Data = playing;
        PlayingRing.Visibility = Visibility.Collapsed;
        Unloaded += (_, _) => SetActive(false);
    }

    public void SetTracks(IReadOnlyList<LibraryTrack> value)
    {
        if (disposed) return;
        var next = value.ToArray();
        bool changed = !tracks.Select(track => track.Path).SequenceEqual(next.Select(track => track.Path), StringComparer.OrdinalIgnoreCase);
        foreach (var track in tracks) track.PropertyChanged -= TrackChanged;
        tracks = next;
        foreach (var track in tracks) track.PropertyChanged += TrackChanged;
        FindTrack.ItemsSource = tracks;
        if (!changed) { UpdatePlaying(); if (active && engine == null) BeginBuild(); return; }
        ++revision;
        StopRendering();
        ReleaseEngine();
        hasFrame = false; hover = -1; hoverSegments.Clear();
        HoverEdges.Data = null;
        RenderCallbackCount = SnapshotCount = 0;
        RenderTotalMilliseconds = RenderWorstMilliseconds = 0;
        points = Array.Empty<GraphPoint>(); circles = Array.Empty<EllipseGeometry>(); segments = Array.Empty<(PathFigure, LineSegment)>();
        foreach (var path in new[] { SoftEdges, MediumEdges, StrongEdges, HoverEdges, QuietNodes, MiddleNodes, HubNodes }) path.Data = null;
        PlayingRing.Visibility = HoverLabel.Visibility = Visibility.Collapsed;
        EmptyLabel.Visibility = Visibility.Visible;
        EmptyLabel.Text = tracks.Length == 0 ? "Choose a media folder to explore its connections." : "Connecting filenames…";
        if (active) BeginBuild();
    }

    public void SetActive(bool value)
    {
        if (disposed || active == value) return;
        active = value;
        if (!active)
        {
            StopRendering();
            dragging = pinching = false; pin = -1;
            Viewport.ReleasePointerCaptures();
            return;
        }
        if (engine == null) BeginBuild();
        else { UpdateTransforms(); UpdatePlaying(); awake = true; StartRendering(); }
    }

    private async void BeginBuild()
    {
        if (building || disposed || !active || tracks.Length == 0) return;
        int generation = revision;
        string[] paths = tracks.Select(track => track.Path).ToArray();
        building = true;
        GraphInfo.Text = $"Connecting {paths.Length:N0} local files…";
        try
        {
            var built = await Task.Run(() => GraphEngine.Build(paths));
            if (disposed || generation != revision || !active) { _ = Task.Run(built.Dispose); return; }
            engine = built;
            points = new GraphPoint[built.Count];
            BuildGeometry();
            needsFit = true;
            EmptyLabel.Text = "Arranging connections…";
            GraphInfo.Text = $"{built.Count:N0} files · {built.Edges.Length:N0} filename connections";
            ResetMotion();
        }
        catch (Exception error) { if (!disposed && generation == revision) Report(error); }
        finally
        {
            building = false;
            if (!disposed && generation != revision && active && engine == null) BeginBuild();
        }
    }

    private void BuildGeometry()
    {
        if (engine == null) return;
        var edgeGroups = new[] { new PathGeometry(), new PathGeometry(), new PathGeometry() };
        segments = new (PathFigure, LineSegment)[engine.Edges.Length];
        for (int i = 0; i < engine.Edges.Length; ++i)
        {
            var figure = new PathFigure { IsClosed = false, IsFilled = false };
            var end = new LineSegment(); figure.Segments.Add(end);
            edgeGroups[engine.Edges[i].Strength >= .6 ? 2 : engine.Edges[i].Strength >= .25 ? 1 : 0].Figures.Add(figure);
            segments[i] = (figure, end);
        }
        SoftEdges.Data = edgeGroups[0]; MediumEdges.Data = edgeGroups[1]; StrongEdges.Data = edgeGroups[2];
        var nodeGroups = new[] { new GeometryGroup { FillRule = FillRule.Nonzero }, new GeometryGroup { FillRule = FillRule.Nonzero }, new GeometryGroup { FillRule = FillRule.Nonzero } };
        circles = new EllipseGeometry[engine.Count];
        for (int i = 0; i < circles.Length; ++i)
        {
            circles[i] = new EllipseGeometry();
            nodeGroups[engine.Weights[i] >= .7 ? 2 : engine.Weights[i] >= .3 ? 1 : 0].Children.Add(circles[i]);
        }
        QuietNodes.Data = nodeGroups[0]; MiddleNodes.Data = nodeGroups[1]; HubNodes.Data = nodeGroups[2];
    }

    private void StartRendering()
    {
        if (rendering || disposed || !active || engine == null) return;
        clock.Restart(); lastTick = 0;
        CompositionTarget.Rendering += Render;
        rendering = true;
    }
    private void StopRendering()
    {
        if (!rendering) return;
        CompositionTarget.Rendering -= Render;
        rendering = false; clock.Stop();
    }
    private void Render(object? sender, object args)
    {
        if (!active || disposed || engine == null) { StopRendering(); return; }
        long started = Stopwatch.GetTimestamp();
        try
        {
            double now = clock.Elapsed.TotalSeconds, dt = Math.Clamp(now - lastTick, 0, .05);
            lastTick = now;
            bool received = engine.Take(points, out bool moving);
            if (received)
            {
                ++SnapshotCount;
                hasFrame = true;
                EmptyLabel.Visibility = Visibility.Collapsed;
                awake = moving;
                if (pin >= 0 && pin < points.Length) points[pin] = pinned;
                if (needsFit && Viewport.ActualWidth > 0 && Viewport.ActualHeight > 0)
                { Fit(); needsFit = false; }
            }
            if (revealing && reveal.Elapsed.TotalSeconds > 1.8) revealing = false;
            if (received || revealing) UpdateGeometry();
            if (awake || pin >= 0) engine.Request(dt, pin, pinned);
            else if (!revealing) StopRendering();
        }
        catch (Exception error) { StopRendering(); Report(error); }
        finally
        {
            double milliseconds = Stopwatch.GetElapsedTime(started).TotalMilliseconds;
            ++RenderCallbackCount;
            RenderTotalMilliseconds += milliseconds;
            RenderWorstMilliseconds = Math.Max(RenderWorstMilliseconds, milliseconds);
        }
    }

    private void UpdateGeometry()
    {
        if (engine == null) return;
        for (int i = 0; i < points.Length; ++i)
        {
            circles[i].Center = new Point(points[i].X, points[i].Y);
            double progress = revealing ? Math.Max(0, reveal.Elapsed.TotalSeconds - .6 * i / Math.Max(1, points.Length - 1)) : 10;
            double pop = revealing ? Math.Max(0, 1 - Math.Exp(-7 * progress) * Math.Cos(11 * progress)) : 1;
            circles[i].RadiusX = circles[i].RadiusY = Radius(i) * pop;
        }
        for (int i = 0; i < segments.Length; ++i)
        {
            var edge = engine.Edges[i];
            segments[i].Figure.StartPoint = new Point(points[edge.A].X, points[edge.A].Y);
            segments[i].End.Point = new Point(points[edge.B].X, points[edge.B].Y);
        }
        UpdatePlaying(); UpdateHoverEdges(); PositionLabel();
    }
    private double Radius(int i) => Math.Clamp((6 + 12 * Math.Pow(engine?.Weights[i] ?? 0, .72)) * view.Scale, 2, 28) / view.Scale;
    private void UpdateTransforms()
    {
        var matrix = new Matrix(view.Scale, 0, 0, view.Scale, Viewport.ActualWidth / 2 + view.PanX, Viewport.ActualHeight / 2 + view.PanY);
        foreach (var transform in transforms) transform.Matrix = matrix;
        for (int i = 0; i < circles.Length; ++i) circles[i].RadiusX = circles[i].RadiusY = Radius(i);
        SoftEdges.StrokeThickness = MediumEdges.StrokeThickness = 1 / view.Scale;
        StrongEdges.StrokeThickness = 1.1 / view.Scale;
        HoverEdges.StrokeThickness = 1.3 / view.Scale;
        PlayingRing.StrokeThickness = 2 / view.Scale;
        UpdatePlaying(); PositionLabel();
    }
    private void TrackChanged(object? sender, PropertyChangedEventArgs args)
    { if (args.PropertyName == nameof(LibraryTrack.IsCurrent)) UpdatePlaying(); }
    private void UpdatePlaying()
    {
        int index = Array.FindIndex(tracks, track => track.IsCurrent);
        if (index < 0 || index >= points.Length || needsFit)
        { PlayingRing.Visibility = Visibility.Collapsed; return; }
        playing.Center = new Point(points[index].X, points[index].Y);
        playing.RadiusX = playing.RadiusY = Radius(index) + 4 / view.Scale;
        PlayingRing.Visibility = Visibility.Visible;
    }
    private void Fit()
    {
        view.Fit(points, Viewport.ActualWidth, Viewport.ActualHeight);
        UpdateTransforms();
    }
    private void FitClicked(object sender, RoutedEventArgs args) => Fit();
    private void PlayingClicked(object sender, RoutedEventArgs args) => CenterTrack(Array.FindIndex(tracks, track => track.IsCurrent));
    private void FindSelectionChanged(object sender, SelectionChangedEventArgs args) => CenterTrack(FindTrack.SelectedIndex);
    private void CenterTrack(int index)
    {
        if (index < 0 || index >= points.Length || needsFit) return;
        view.Center(points[index]); UpdateTransforms(); SetHover(index);
    }
    private void WandClicked(object sender, RoutedEventArgs args)
    {
        if (engine == null) { BeginBuild(); return; }
        revealing = systemUi.AnimationsEnabled;
        reveal.Restart(); ResetMotion();
    }
    private void ForcesChanged(object sender, RangeBaseValueChangedEventArgs args)
    { if (!changingForces && engine != null) ResetMotion(); }
    private void ResetForcesClicked(object sender, RoutedEventArgs args)
    {
        changingForces = true;
        CenterForce.Value = RepelForce.Value = LinkForce.Value = LinkDistance.Value = 100;
        changingForces = false;
        ResetMotion();
    }
    private void ResetMotion()
    {
        if (engine == null) return;
        try
        {
            // Respect the OS motion preference without moving simulation onto
            // the UI thread. Reduced-motion reset settles once on the worker.
            bool animate = systemUi.AnimationsEnabled;
            if (!animate) revealing = false;
            engine.Reset((int)CenterForce.Value, (int)RepelForce.Value, (int)LinkForce.Value, (int)LinkDistance.Value, settle: !animate);
            awake = true; StartRendering();
        }
        catch (Exception error) { Report(error); }
    }

    private GraphPoint Position(PointerRoutedEventArgs args)
    { var value = args.GetCurrentPoint(Viewport).Position; return new GraphPoint(value.X, value.Y); }
    private int Hit(GraphPoint screen)
    {
        int nearest = -1; double distance = double.PositiveInfinity;
        for (int i = 0; i < points.Length; ++i)
        {
            var location = view.Screen(points[i], Viewport.ActualWidth, Viewport.ActualHeight);
            double dx = screen.X - location.X, dy = screen.Y - location.Y;
            double delta = Math.Sqrt(dx * dx + dy * dy);
            if (delta <= Math.Max(9, Radius(i) * view.Scale + 3) && delta < distance) { nearest = i; distance = delta; }
        }
        return nearest;
    }
    private void GraphPointerPressed(object sender, PointerRoutedEventArgs args)
    {
        if (engine == null || needsFit || dragging || pinching) return;
        if (args.Pointer.PointerDeviceType == Microsoft.UI.Input.PointerDeviceType.Mouse && !args.GetCurrentPoint(Viewport).Properties.IsLeftButtonPressed) return;
        if (!Viewport.CapturePointer(args.Pointer)) return;
        dragging = true; moved = false;
        pressed = pointer = Position(args); pin = Hit(pointer);
        var world = view.World(pointer, Viewport.ActualWidth, Viewport.ActualHeight);
        grabOffset = pin >= 0 ? new GraphPoint(points[pin].X - world.X, points[pin].Y - world.Y) : new GraphPoint();
        pinned = pin >= 0 ? points[pin] : world;
        if (pin >= 0) { awake = true; StartRendering(); }
        args.Handled = true;
    }
    private void GraphPointerMoved(object sender, PointerRoutedEventArgs args)
    {
        var next = Position(args);
        if (!dragging || pinching) { SetHover(Hit(next)); return; }
        moved |= (next.X - pressed.X) * (next.X - pressed.X) + (next.Y - pressed.Y) * (next.Y - pressed.Y) > 16;
        if (pin >= 0)
        {
            var world = view.World(next, Viewport.ActualWidth, Viewport.ActualHeight);
            pinned = new GraphPoint(world.X + grabOffset.X, world.Y + grabOffset.Y);
            points[pin] = pinned; UpdateGeometry();
        }
        else { view.Pan(next.X - pointer.X, next.Y - pointer.Y); UpdateTransforms(); }
        pointer = next; args.Handled = true;
    }
    private void GraphPointerReleased(object sender, PointerRoutedEventArgs args)
    {
        if (!dragging) return;
        int chosen = !moved && !pinching ? pin : -1;
        dragging = false; pin = -1;
        Viewport.ReleasePointerCapture(args.Pointer);
        if (awake) StartRendering();
        if (chosen >= 0 && chosen < tracks.Length) TrackRequested?.Invoke(tracks[chosen]);
        args.Handled = true;
    }
    private void GraphPointerCanceled(object sender, PointerRoutedEventArgs args)
    { dragging = false; pin = -1; if (active && awake) StartRendering(); }
    private void GraphPointerExited(object sender, PointerRoutedEventArgs args) { if (!dragging) SetHover(-1); }
    private void GraphWheelChanged(object sender, PointerRoutedEventArgs args)
    {
        view.ZoomAt(Position(args), Math.Pow(1.15, args.GetCurrentPoint(Viewport).Properties.MouseWheelDelta / 120d), Viewport.ActualWidth, Viewport.ActualHeight);
        UpdateTransforms(); args.Handled = true;
    }
    private void GraphManipulationStarted(object sender, ManipulationStartedRoutedEventArgs args)
    { pinching = true; dragging = false; pin = -1; }
    private void GraphManipulationDelta(object sender, ManipulationDeltaRoutedEventArgs args)
    {
        view.ZoomAt(new GraphPoint(args.Position.X, args.Position.Y), args.Delta.Scale, Viewport.ActualWidth, Viewport.ActualHeight);
        UpdateTransforms(); args.Handled = true;
    }
    private void GraphManipulationCompleted(object sender, ManipulationCompletedRoutedEventArgs args) { pinching = false; }
    private void ViewportSizeChanged(object sender, SizeChangedEventArgs args)
    {
        Viewport.Clip = new RectangleGeometry { Rect = new Rect(0, 0, Math.Max(0, args.NewSize.Width), Math.Max(0, args.NewSize.Height)) };
        if (hasFrame && points.Length > 0 && needsFit) { Fit(); needsFit = false; }
        else UpdateTransforms();
    }
    private void PageSizeChanged(object sender, SizeChangedEventArgs args)
    {
        PageRoot.Clip = new RectangleGeometry { Rect = new Rect(0, 0, Math.Max(0, args.NewSize.Width), Math.Max(0, args.NewSize.Height)) };
        if (ControlsScroll != null) ControlsScroll.MaxHeight = Math.Clamp(args.NewSize.Height * .22, 36, 180);
    }
    private void SetHover(int index)
    {
        if (hover == index) return;
        hover = index;
        HoverLabel.Visibility = index >= 0 ? Visibility.Visible : Visibility.Collapsed;
        if (index >= 0) HoverTitle.Text = tracks[index].Title;
        hoverSegments.Clear();
        var geometry = new PathGeometry();
        if (hover >= 0 && engine != null)
            for (int i = 0; i < engine.Edges.Length; ++i)
                if (engine.Edges[i].A == hover || engine.Edges[i].B == hover)
                {
                    var figure = new PathFigure { IsFilled = false };
                    var end = new LineSegment(); figure.Segments.Add(end);
                    geometry.Figures.Add(figure); hoverSegments.Add((i, figure, end));
                }
        HoverEdges.Data = geometry;
        UpdateHoverEdges(); PositionLabel();
    }
    private void UpdateHoverEdges()
    {
        if (engine == null) return;
        foreach (var item in hoverSegments)
        {
            var edge = engine.Edges[item.Index];
            item.Figure.StartPoint = new Point(points[edge.A].X, points[edge.A].Y);
            item.End.Point = new Point(points[edge.B].X, points[edge.B].Y);
        }
    }
    private void PositionLabel()
    {
        if (hover < 0 || hover >= points.Length) return;
        var position = view.Screen(points[hover], Viewport.ActualWidth, Viewport.ActualHeight);
        Canvas.SetLeft(HoverLabel, Math.Clamp(position.X + 14, 4, Math.Max(4, Viewport.ActualWidth - 284)));
        Canvas.SetTop(HoverLabel, Math.Clamp(position.Y + 12, 4, Math.Max(4, Viewport.ActualHeight - 64)));
    }
    private void Report(Exception error)
    { App.RecordFailure(error); GraphInfo.Text = "Graph unavailable · " + error.Message; EmptyLabel.Text = "Try refreshing the library."; EmptyLabel.Visibility = Visibility.Visible; }
    private void ReleaseEngine()
    { var previous = engine; engine = null; if (previous != null) _ = Task.Run(previous.Dispose); }
    public void Dispose()
    {
        if (disposed) return;
        SetActive(false); disposed = true; ++revision;
        foreach (var track in tracks) track.PropertyChanged -= TrackChanged;
        ReleaseEngine();
    }
}
