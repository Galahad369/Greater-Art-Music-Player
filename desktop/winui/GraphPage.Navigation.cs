using Microsoft.UI.Input;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Windows.Foundation;
using Windows.System;
using Windows.UI.Core;

namespace GreaterArt.Desktop;

public sealed partial class GraphPage
{
    public void FocusCanvas() => Viewport.Focus(FocusState.Programmatic);

    public object VerificationGeometry() => new
    {
        scale = view.Scale, width = Viewport.ActualWidth, height = Viewport.ActualHeight,
        nodes = points.Select((point, index) => new { index, world = point,
            screen = view.Screen(point, Viewport.ActualWidth, Viewport.ActualHeight),
            radius = Radius(index) * view.Scale, visible = filtered?.Visible[index] == true,
            weight = filtered?.Weights[index] }).ToArray(),
        layers = new[] { QuietNodes, MiddleNodes, HubNodes, SoftEdges, MediumEdges, StrongEdges }
            .Select(path => new { path.Name, path.ActualWidth, path.ActualHeight,
                bounds = path.Data?.Bounds, origin = path.TransformToVisual(Viewport).TransformPoint(new Point()) }).ToArray()
    };

    private void PageKeyDown(object sender, KeyRoutedEventArgs args)
    {
        // Canvas navigation must not consume arrows or typing inside settings.
        if (args.Key == VirtualKey.Escape && SettingsPanel.Visibility == Visibility.Visible)
        {
            SetSettingsOpen(false); FocusCanvas(); args.Handled = true;
        }
    }

    private void GraphKeyDown(object sender, KeyRoutedEventArgs args)
    {
        bool shift = InputKeyboardSource.GetKeyStateForCurrentThread(VirtualKey.Shift).HasFlag(CoreVirtualKeyStates.Down);
        args.Handled = Navigate(args.Key, shift);
    }
    private bool Navigate(VirtualKey key, bool shift = false)
    {
        double step = shift ? 120 : 40;
        switch (key)
        {
            case VirtualKey.Space: spacePan = true; return true;
            case VirtualKey.Left: view.Pan(-step, 0); break;
            case VirtualKey.Right: view.Pan(step, 0); break;
            case VirtualKey.Up: view.Pan(0, -step); break;
            case VirtualKey.Down: view.Pan(0, step); break;
            case VirtualKey.Home: Fit(); return true;
            case VirtualKey.Enter: PlaySelectedClicked(this, new RoutedEventArgs()); return true;
            case VirtualKey.Escape:
                if (SettingsPanel.Visibility == Visibility.Visible)
                { SetSettingsOpen(false); }
                else if (LocalMode.IsChecked == true) { LocalMode.IsChecked = false; ApplyExploration(); }
                return true;
            default:
                int code = (int)key;
                if (code is not (107 or 187 or 109 or 189)) return false;
                view.ZoomAt(new GraphPoint(Viewport.ActualWidth / 2, Viewport.ActualHeight / 2),
                    code is 107 or 187 ? 1.2 : 1 / 1.2, Viewport.ActualWidth, Viewport.ActualHeight);
                break;
        }
        UpdateTransforms(); return true;
    }
    private void GraphKeyUp(object sender, KeyRoutedEventArgs args)
    { if (args.Key == VirtualKey.Space) { spacePan = false; args.Handled = true; } }
    private void GraphLostFocus(object sender, RoutedEventArgs args) { spacePan = false; }

    private void UpdateLabels()
    {
        if (filtered == null || !hasFrame || needsFit) return;
        bool showAll = view.Scale >= LabelThreshold.Value;
        labelCells.Clear();
        var priorities = new List<int>();
        if (hover >= 0)
        {
            priorities.Add(hover);
            foreach (var item in hoverSegments)
            {
                var edge = engine!.Edges[item.Index]; priorities.Add(edge.A); priorities.Add(edge.B);
            }
        }
        if (selected >= 0) priorities.Add(selected);
        int current = Array.FindIndex(tracks, track => track.IsCurrent);
        if (current >= 0) priorities.Add(current);
        if (showAll) priorities.AddRange(labelOrder);
        int used = 0;
        foreach (int i in priorities.Distinct())
        {
            if (i >= points.Length || !filtered.Visible[i]) continue;
            var position = view.Screen(points[i], Viewport.ActualWidth, Viewport.ActualHeight);
            double width = Math.Clamp(tracks[i].Title.Length * 12d, 40, 220);
            double x = position.X + Radius(i) * view.Scale + 6, y = position.Y - 10;
            if (x < 0 || y < 0 || x + width > Viewport.ActualWidth || y + 20 > Viewport.ActualHeight - 25) continue;
            if (SettingsPanel.Visibility == Visibility.Visible && x + width > Viewport.ActualWidth - SettingsPanel.ActualWidth - 12) continue;
            int left = (int)(x / 32), right = (int)((x + width) / 32), top = (int)(y / 22), bottom = (int)((y + 20) / 22);
            bool blocked = false;
            for (int column = left; column <= right && !blocked; ++column)
                for (int row = top; row <= bottom; ++row) if (labelCells.Contains((column, row))) { blocked = true; break; }
            if (blocked) continue;
            for (int column = left; column <= right; ++column)
                for (int row = top; row <= bottom; ++row) labelCells.Add((column, row));
            if (used == labels.Count)
            {
                var label = new TextBlock { FontSize = 12, MaxLines = 1, TextTrimming = TextTrimming.CharacterEllipsis,
                    IsHitTestVisible = false };
                labels.Add(label); LabelLayer.Children.Add(label);
            }
            var text = labels[used++];
            if (text.Tag is not int previous || previous != i) { text.Text = tracks[i].Title; text.Tag = i; }
            text.Width = width; text.Visibility = Visibility.Visible;
            text.Opacity = i == hover || i == selected || i == current ? 1 : Math.Clamp((view.Scale - LabelThreshold.Value) * 2 + .4, .4, .85);
            Canvas.SetLeft(text, x); Canvas.SetTop(text, y);
        }
        for (int i = used; i < labels.Count; ++i) labels[i].Visibility = Visibility.Collapsed;
    }

    // Runs the real controls/model inside the owned application test. No physical
    // input or audible acceptance is inferred from these programmatic actions.
    public async Task<IReadOnlyDictionary<string, bool>> VerifyExplorationControls()
    {
        if (!IsReady || engine == null) throw new InvalidOperationException("The graph has no complete frame.");
        var saved = SnapshotPreferences();
        var savedTracks = tracks;
        int previousSelection = selected;
        var checks = new Dictionary<string, bool>();
        try
        {
            SetSettingsOpen(true);
            GraphBody.UpdateLayout();
            await Task.Delay(150);
            checks["filter control is loaded"] = FilterQuery.IsLoaded && FilterQuery.ActualWidth > 0;
            RestorePreferences(new()); LocalMode.IsChecked = false; ApplyExploration();
            checks["all nodes retained"] = filtered!.VisibleCount == tracks.Length;
            if (labelOrder.Length > 1 && filtered.Weights[labelOrder[0]] > filtered.Weights[labelOrder[^1]] + .1)
                checks["hubs stay larger at fitted zoom"] = Radius(labelOrder[0]) > Radius(labelOrder[^1]);
            if (tracks.Length > 0)
            {
                selected = 0;
                await SetFilter(tracks[0].Title);
                checks["filename filter applies"] = filtered!.Visible[0] && filtered.VisibleCount <= tracks.Length;
                await SetFilter("__greater_art_no_matching_filename_37f4__");
                checks["empty filter hides nodes and edges"] = filtered!.VisibleCount == 0 && filtered.EdgeIndices.Length == 0;
                UpdateEmptyState();
                checks["empty state stays readable"] = EmptyLabel.Visibility == Visibility.Visible;
                await SetFilter("");
                LocalMode.IsChecked = true; LocalModeChanged(this, new RoutedEventArgs());
                checks["local view uses neighbor depth"] = filtered!.Visible[0] && filtered.VisibleCount <= tracks.Length;
                LocalMode.IsChecked = false; ApplyExploration();
                int other = Array.FindIndex(tracks, track => !exploration!.Compute(track.Title).Visible[0]);
                if (other >= 0)
                {
                    await SetFilter(tracks[other].Title);
                    LocalMode.IsChecked = true; LocalModeChanged(this, new RoutedEventArgs());
                    checks["local center uses a visible track"] = selected >= 0 && filtered!.Visible[selected] && filtered.VisibleCount > 0;
                    LocalMode.IsChecked = false;
                    await SetFilter(""); selected = 0; ApplyExploration();
                }
                groups.Add(new(tracks[0].Title, 0xffeab4dc)); ApplyExploration(false);
                checks["group color applies"] = filtered!.GroupColors[0] == 0xffeab4dc;
                groups.Clear(); ApplyExploration(false);
                CenterTrack(0);
                checks["selector follows canvas selection"] = ReferenceEquals(FindTrack.SelectedItem, tracks[0]);
                SetTracks(tracks.Select((track, index) => index == 0 ? new LibraryTrack(track.Path) : track).ToArray());
                checks["refresh rebinds track selector"] = ReferenceEquals(FindTrack.SelectedItem, tracks[0]);
                ShowOrphans.IsChecked = false; ApplyExploration();
                checks["orphan filter applies"] = Enumerable.Range(0, tracks.Length).All(i => !filtered!.Visible[i] || filtered.Degrees[i] > 0);
                ShowOrphans.IsChecked = true; ApplyExploration();
                LabelThreshold.Value = .1;
                view.ZoomAt(new GraphPoint(Viewport.ActualWidth / 2, Viewport.ActualHeight / 2), 5, Viewport.ActualWidth, Viewport.ActualHeight);
                view.Center(points[0]);
                UpdateTransforms();
                checks["zoom reveals names"] = labels.Any(label => label.Visibility == Visibility.Visible);
                double before = view.PanX; Navigate(VirtualKey.Right);
                checks["keyboard pan"] = view.PanX > before;
                Navigate(VirtualKey.Space); checks["space is canvas pan"] = spacePan; spacePan = false;
                SettingsButton.IsChecked = true; SettingsClicked(this, new RoutedEventArgs());
                checks["settings panel stays in canvas"] = SettingsPanel.Width <= GraphBody.ActualWidth && ControlsScroll.MaxHeight <= GraphBody.ActualHeight;
                SettingsButton.IsChecked = false; SettingsClicked(this, new RoutedEventArgs());
            }
        }
        finally
        {
            SetTracks(savedTracks);
            RestorePreferences(saved); selected = previousSelection;
            LocalMode.IsChecked = false; RebuildGroupRows(); ApplyExploration(); Fit();
        }
        return checks;

        async Task SetFilter(string text)
        {
            if (FilterQuery.Text == text) return;
            var changed = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
            TextChangedEventHandler handler = (_, _) => changed.TrySetResult();
            FilterQuery.TextChanged += handler;
            try
            {
                FilterQuery.Text = text;
                // Check the real control event, not state before XAML delivers it.
                await changed.Task.WaitAsync(TimeSpan.FromSeconds(2));
            }
            finally { FilterQuery.TextChanged -= handler; }
        }
    }
}
