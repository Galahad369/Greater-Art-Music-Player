using Microsoft.UI;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Automation;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Media;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;

namespace GreaterArt.Desktop;

public sealed partial class GraphPage
{
    private bool controlsReady, updatingControls;
    private readonly List<GraphColorGroup> groups = new();
    private readonly DispatcherTimer saveTimer = new() { Interval = TimeSpan.FromMilliseconds(450) };
    private readonly bool verification = Environment.GetCommandLineArgs().Contains("--verify-flow");
    private static string PreferencesPath => Path.Combine(App.DataFolder, "nodes-v1.json");

    private sealed class Preferences
    {
        public Preferences() { }
        public string Query { get; set; } = "";
        public bool ShowOrphans { get; set; } = true;
        public double MinimumLink { get; set; }
        public int Depth { get; set; } = 1;
        public double NodeSize { get; set; } = 1;
        public double LinkWidth { get; set; } = 1;
        public double LabelThreshold { get; set; } = .7;
        public int Center { get; set; } = 100;
        public int Repel { get; set; } = 100;
        public int Link { get; set; } = 100;
        public int Distance { get; set; } = 100;
        public List<GraphColorGroup> Groups { get; set; } = new();
    }

    private void InitializeControls()
    {
        saveTimer.Tick += (_, _) => { saveTimer.Stop(); SavePreferences(); };
        if (!verification && File.Exists(PreferencesPath))
        {
            try { RestorePreferences(JsonSerializer.Deserialize<Preferences>(File.ReadAllText(PreferencesPath)) ?? new()); }
            catch (Exception error) when (error is IOException or UnauthorizedAccessException or JsonException)
            { App.RecordFailure(error); }
        }
        RebuildGroupRows();
        controlsReady = true;
    }

    private Preferences SnapshotPreferences() => new()
    {
        Query = FilterQuery.Text, ShowOrphans = ShowOrphans.IsChecked == true,
        MinimumLink = MinimumLink.Value, Depth = (int)LocalDepth.Value,
        NodeSize = NodeSize.Value, LinkWidth = LinkWidth.Value, LabelThreshold = LabelThreshold.Value,
        Center = (int)CenterForce.Value, Repel = (int)RepelForce.Value,
        Link = (int)LinkForce.Value, Distance = (int)LinkDistance.Value, Groups = groups.ToList()
    };

    private void RestorePreferences(Preferences value)
    {
        updatingControls = changingForces = true;
        FilterQuery.Text = value.Query ?? ""; ShowOrphans.IsChecked = value.ShowOrphans;
        MinimumLink.Value = Finite(value.MinimumLink, 0, 1, 0);
        LocalDepth.Value = Math.Clamp(value.Depth, 1, 3);
        NodeSize.Value = Finite(value.NodeSize, .5, 2, 1);
        LinkWidth.Value = Finite(value.LinkWidth, .5, 3, 1);
        LabelThreshold.Value = Finite(value.LabelThreshold, .1, 2, .7);
        CenterForce.Value = Math.Clamp(value.Center, 25, 200); RepelForce.Value = Math.Clamp(value.Repel, 25, 200);
        LinkForce.Value = Math.Clamp(value.Link, 25, 200); LinkDistance.Value = Math.Clamp(value.Distance, 25, 200);
        groups.Clear();
        if (value.Groups != null)
            foreach (var group in value.Groups)
                if (group != null && group.Query != null) groups.Add(group with { Argb = group.Argb | 0xff000000 });
        updatingControls = changingForces = false;
        static double Finite(double number, double low, double high, double fallback) =>
            double.IsFinite(number) ? Math.Clamp(number, low, high) : fallback;
    }

    private void ScheduleSave()
    {
        if (!controlsReady || updatingControls || verification) return;
        saveTimer.Stop(); saveTimer.Start();
    }
    private void SavePreferences()
    {
        if (!controlsReady || verification) return;
        try
        {
            WritePreferences(PreferencesPath, SnapshotPreferences());
        }
        catch (Exception error) when (error is IOException or UnauthorizedAccessException)
        { App.RecordFailure(error); }
    }
    private static void WritePreferences(string path, Preferences preferences)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        string pending = path + ".tmp";
        File.WriteAllText(pending, JsonSerializer.Serialize(preferences));
        File.Move(pending, path, true);
    }

    public IReadOnlyDictionary<string, bool> VerifyPreferenceStorage(string directory)
    {
        var saved = SnapshotPreferences();
        string file = Path.Combine(directory, "nodes-preference-roundtrip.json");
        try
        {
            var fixture = new Preferences { Query = "孤独 SYUDOU", NodeSize = 1.4, Depth = 3,
                Groups = new() { new("中文", 0xff93c8ef) } };
            WritePreferences(file, fixture);
            var restored = JsonSerializer.Deserialize<Preferences>(File.ReadAllText(file))
                ?? throw new InvalidOperationException("Graph preferences were not restored.");
            RestorePreferences(restored);
            return new Dictionary<string, bool>
            {
                ["Unicode query persists"] = FilterQuery.Text == fixture.Query,
                ["display and depth persist"] = NodeSize.Value == 1.4 && LocalDepth.Value == 3,
                ["group query and color persist"] = groups.Count == 1 && groups[0] == fixture.Groups[0],
                ["atomic write completes"] = !File.Exists(file + ".tmp")
            };
        }
        finally { RestorePreferences(saved); RebuildGroupRows(); ApplyExploration(false); }
    }
    private void SaveOnClose()
    {
        bool pending = saveTimer.IsEnabled; saveTimer.Stop();
        if (pending) SavePreferences();
    }

    private void ApplyExploration(bool fit = true)
    {
        if (engine == null || exploration == null || !controlsReady) return;
        CommitDrag(); hover = -1; hoverSegments.Clear(); HoverEdges.Data = null;
        HoverLabel.Visibility = Visibility.Collapsed;
        int? center = LocalMode.IsChecked == true && selected >= 0 ? selected : null;
        filtered = exploration.Compute(FilterQuery.Text, ShowOrphans.IsChecked == true,
            MinimumLink.Value, center, (int)LocalDepth.Value, groups);
        labelOrder = Enumerable.Range(0, tracks.Length).Where(index => filtered.Visible[index])
            .OrderByDescending(index => filtered.Weights[index]).ToArray();
        updatingControls = true;
        FindTrack.ItemsSource = tracks.Where((_, index) => filtered.Visible[index]).ToArray();
        FindTrack.SelectedItem = selected >= 0 && filtered.Visible[selected] ? tracks[selected] : null;
        updatingControls = false;
        PlaySelectedButton.IsEnabled = selected >= 0 && filtered.Visible[selected];
        LocalDepth.IsEnabled = LocalMode.IsChecked == true;
        GraphInfo.Text = $"{filtered.VisibleCount:N0} / {tracks.Length:N0} files · {filtered.EdgeIndices.Length:N0} connections"
            + (center.HasValue ? " · local" : "");
        BuildGeometry();
        UpdateEmptyState();
        if (fit && hasFrame) Fit();
        if (hasFrame) { UpdateGeometry(); UpdateTransforms(); }
        ScheduleSave();
    }

    private void UpdateEmptyState()
    {
        EmptyLabel.Text = filtered?.VisibleCount == 0 ? "No matching tracks. Clear the filter or widen this neighborhood." : "Arranging connections…";
        EmptyLabel.Visibility = hasFrame && filtered?.VisibleCount > 0 ? Visibility.Collapsed : Visibility.Visible;
    }

    private void SettingsClicked(object sender, RoutedEventArgs args) => SetSettingsOpen(SettingsButton.IsChecked == true);
    public void SetSettingsOpen(bool value)
    {
        SettingsButton.IsChecked = value;
        SettingsPanel.Visibility = value ? Visibility.Visible : Visibility.Collapsed;
        UpdatePanelBounds(); UpdateLabels();
    }
    private void FilterChanged(object sender, TextChangedEventArgs args)
    { if (controlsReady && !updatingControls) { ApplyExploration(); ScheduleSave(); } }
    private void FilterOptionChanged(object sender, RoutedEventArgs args)
    { if (controlsReady && !updatingControls) { ApplyExploration(); ScheduleSave(); } }
    private void FilterValueChanged(object sender, RangeBaseValueChangedEventArgs args)
    { if (controlsReady && !updatingControls) { ApplyExploration(); ScheduleSave(); } }
    private void LocalModeChanged(object sender, RoutedEventArgs args)
    {
        if (!controlsReady || updatingControls) return;
        if (LocalMode.IsChecked == true)
        {
            if (selected < 0 || filtered?.Visible[selected] != true)
            {
                int current = Array.FindIndex(tracks, track => track.IsCurrent);
                selected = current >= 0 && filtered?.Visible[current] == true
                    ? current : labelOrder.FirstOrDefault(-1);
            }
            if (selected < 0) LocalMode.IsChecked = false;
        }
        ApplyExploration();
    }
    private void DisplayChanged(object sender, RangeBaseValueChangedEventArgs args)
    {
        if (!controlsReady || updatingControls) return;
        UpdateTransforms(); ScheduleSave();
    }
    private void PlaySelectedClicked(object sender, RoutedEventArgs args)
    {
        if (selected >= 0 && filtered?.Visible[selected] == true) TrackRequested?.Invoke(tracks[selected]);
    }
    private void ResetAllClicked(object sender, RoutedEventArgs args)
    {
        RestorePreferences(new()); LocalMode.IsChecked = false;
        RebuildGroupRows(); ApplyExploration(); ResetMotion(); ScheduleSave();
    }
    private void AddGroupClicked(object sender, RoutedEventArgs args)
    {
        uint[] palette = { 0xff9be0ca, 0xffd7b6f5, 0xffefbd91, 0xff93c8ef, 0xffec9db1 };
        groups.Add(new("", palette[groups.Count % palette.Length]));
        RebuildGroupRows(); ScheduleSave();
    }

    private void RebuildGroupRows()
    {
        GroupRows.Children.Clear();
        for (int i = 0; i < groups.Count; ++i)
        {
            int index = i;
            var row = new Grid { ColumnSpacing = 4 };
            row.ColumnDefinitions.Add(new() { Width = new GridLength(1, GridUnitType.Star) });
            row.ColumnDefinitions.Add(new() { Width = GridLength.Auto }); row.ColumnDefinitions.Add(new() { Width = GridLength.Auto });
            var query = new TextBox { Text = groups[i].Query, PlaceholderText = "Filename contains…", MinWidth = 0 };
            AutomationProperties.SetName(query, $"Group {i + 1} filename query");
            query.TextChanged += (_, _) => { if (row.Parent != GroupRows) return; groups[index] = groups[index] with { Query = query.Text }; ApplyExploration(false); ScheduleSave(); };
            var color = new Button { Width = 36, Height = 36, Padding = new Thickness(7), Content = new Border { Width = 18, Height = 18, CornerRadius = new CornerRadius(9), Background = new SolidColorBrush(ArgbColor(groups[i].Argb)) } };
            AutomationProperties.SetName(color, $"Group {i + 1} color");
            var picker = new ColorPicker { Color = ArgbColor(groups[i].Argb), IsAlphaEnabled = false };
            color.Flyout = new Flyout { Content = picker };
            picker.ColorChanged += (_, args) =>
            {
                if (row.Parent != GroupRows) return;
                uint argb = 0xff000000u | (uint)args.NewColor.R << 16 | (uint)args.NewColor.G << 8 | args.NewColor.B;
                groups[index] = groups[index] with { Argb = argb };
                ((Border)color.Content).Background = new SolidColorBrush(args.NewColor);
                ApplyExploration(false); ScheduleSave();
            };
            var remove = new Button { Width = 32, Height = 36, Padding = new Thickness(5), Content = new FontIcon { Glyph = "\uE711", FontSize = 14 } };
            AutomationProperties.SetName(remove, $"Remove group {i + 1}");
            remove.Click += (_, _) => { if (row.Parent != GroupRows) return; groups.RemoveAt(index); RebuildGroupRows(); ApplyExploration(false); ScheduleSave(); };
            Grid.SetColumn(color, 1); Grid.SetColumn(remove, 2);
            row.Children.Add(query); row.Children.Add(color); row.Children.Add(remove); GroupRows.Children.Add(row);
        }
    }

    private static Windows.UI.Color ArgbColor(uint value) => ColorHelper.FromArgb((byte)(value >> 24),
        (byte)(value >> 16), (byte)(value >> 8), (byte)value);
}
