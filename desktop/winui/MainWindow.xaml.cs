using Microsoft.UI;
using Microsoft.UI.Windowing;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Automation.Peers;
using Microsoft.UI.Xaml.Automation.Provider;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;
using Windows.Graphics;
using Windows.Storage.Pickers;

namespace GreaterArt.Desktop;

public sealed partial class MainWindow : Window
{
    private readonly List<LibraryTrack> library = new();
    private readonly ObservableCollection<LibraryTrack> visibleTracks = new();
    private readonly ObservableCollection<LibraryTrack> queue = new();
    private readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromMilliseconds(120) };
    private nint player, host;
    private nint attachedHost;
    private (nint Host, int X, int Y, int Width, int Height, int Visible)? lastHostBounds;
    private LibraryTrack? current;
    private Window? mini;
    private Border? miniStage;
    private FontIcon? miniPlayIcon;
    private nint miniHost;
    private bool ready, disposed, updatingTimeline, scrubbing, playerPage, nodesPage, fullscreen, repeat = true;
    private string? folder;
    private int scanGeneration;
    private PlaybackState state;
    private bool verifying;
    private ulong lastPaletteSample;
    private string? lastVideoHostError;
    private nint WindowHandle => WinRT.Interop.WindowNative.GetWindowHandle(this);

    public MainWindow()
    {
        InitializeComponent();
        AppWindow.Resize(new SizeInt32(1180, 820));
        var icon = Path.Combine(AppContext.BaseDirectory, "Assets", "app.ico");
        if (File.Exists(icon)) AppWindow.SetIcon(icon);
        StyleTitleBar(AppWindow);
        LibraryList.ItemsSource = visibleTracks;
        QueueList.ItemsSource = queue;
        GraphView.TrackRequested += track => { try { Play(track); } catch (Exception error) { Report(error); } };
        AppWindow.Changed += (_, _) => UpdateGraphActivity();
        timer.Tick += (_, _) => Tick();
        AppWindow.Closing += (_, _) => DisposePlayback();
        Closed += (_, _) => DisposePlayback();
        Root.KeyDown += OnKeyDown;
        Timeline.AddHandler(UIElement.PointerPressedEvent, new PointerEventHandler((_, _) => scrubbing = true), true);
        Timeline.AddHandler(UIElement.PointerReleasedEvent, new PointerEventHandler((_, _) => EndScrub()), true);
        Timeline.AddHandler(UIElement.PointerCaptureLostEvent, new PointerEventHandler((_, _) => EndScrub()), true);
        Timeline.AddHandler(UIElement.PointerCanceledEvent, new PointerEventHandler((_, _) => scrubbing = false), true);
    }

    private async void OnLoaded(object sender, RoutedEventArgs args)
    {
        if (ready) return;
        try
        {
            ResizeEffective(1180, 820);
            if (Engine.GA_Initialize() == 0) throw new InvalidOperationException("The local playback engine could not start.");
            player = Engine.GA_Create();
            if (player == 0) throw new InvalidOperationException("The native player could not be created.");
            host = Engine.GA_CreateHost(WindowHandle);
            if (host == 0) throw new InvalidOperationException("The video surface could not be created.");
            ready = true;
            var arguments = Environment.GetCommandLineArgs();
            int verify = Array.IndexOf(arguments, "--verify-flow");
            verifying = verify >= 0 && arguments.Length > verify + 2;
            var ambientPreference = Path.Combine(App.DataFolder, "ambient.txt");
            Ambient.IsOn = !verifying && File.Exists(ambientPreference) && File.ReadAllText(ambientPreference).Trim() == "on";
            timer.Start();
            Root.LayoutUpdated += (_, _) => UpdateVideoHost();
            UpdateVideoHost();
            if (verifying)
            {
                await VerifyFlow(arguments[verify + 1], arguments[verify + 2]);
                return;
            }
            var preferenceFile = Path.Combine(App.DataFolder, "library-folder.txt");
            if (File.Exists(preferenceFile))
            {
                var saved = File.ReadAllText(preferenceFile).Trim();
                if (Directory.Exists(saved)) await LoadFolder(saved);
            }
        }
        catch (Exception error) { Report(error); }
    }

    private static readonly HashSet<string> Extensions = new(StringComparer.OrdinalIgnoreCase)
    { ".mp3", ".mp4", ".m4a", ".m4v", ".mkv", ".wav", ".flac", ".ogg", ".opus", ".webm", ".aac", ".aiff", ".aif", ".wma", ".mov" };

    private async void ChooseFolder(object sender, RoutedEventArgs args)
    {
        try
        {
            var picker = new FolderPicker();
            picker.FileTypeFilter.Add("*");
            WinRT.Interop.InitializeWithWindow.Initialize(picker, WindowHandle);
            var chosen = await picker.PickSingleFolderAsync();
            if (chosen == null) return;
            if (!await LoadFolder(chosen.Path)) return;
            Directory.CreateDirectory(App.DataFolder);
            File.WriteAllText(Path.Combine(App.DataFolder, "library-folder.txt"), chosen.Path);
        }
        catch (Exception error) { Report(error); }
    }

    private async Task<bool> LoadFolder(string path)
    {
        int generation = ++scanGeneration;
        LibraryInfo.Text = "Reading local files…";
        var tracks = await Task.Run(() =>
        {
            var options = new EnumerationOptions { RecurseSubdirectories = true, IgnoreInaccessible = true,
                AttributesToSkip = FileAttributes.ReparsePoint | FileAttributes.System };
            return Directory.EnumerateFiles(path, "*", options)
                .Where(file => Extensions.Contains(Path.GetExtension(file)))
                .OrderBy(file => Path.GetFileName(file), StringComparer.CurrentCultureIgnoreCase)
                .Select(file => new LibraryTrack(file)).ToList();
        });
        if (disposed || generation != scanGeneration) return false;
        folder = path;
        library.Clear(); library.AddRange(tracks);
        if (current != null) current = library.FirstOrDefault(track => track.Path == current.Path) ?? current;
        foreach (var track in library) track.IsCurrent = track.Path == current?.Path;
        FilterLibrary();
        ReplaceQueue(library);
        GraphView.SetTracks(library);
        FolderInfo.Text = path;
        LibraryInfo.Text = $"{library.Count:N0} files · local & private";
        return true;
    }

    private void FilterLibrary()
    {
        visibleTracks.Clear();
        foreach (var track in library.Where(track => track.Title.Contains(Search.Text.Trim(), StringComparison.CurrentCultureIgnoreCase))) visibleTracks.Add(track);
    }
    private void ReplaceQueue(IEnumerable<LibraryTrack> tracks)
    {
        queue.Clear(); foreach (var track in tracks) queue.Add(track);
    }
    private void FilterChanged(object sender, TextChangedEventArgs args) { if (LibraryList != null) FilterLibrary(); }
    private void QueueFilterChanged(object sender, TextChangedEventArgs args) => ReplaceQueue(library.Where(track => track.Title.Contains(QueueSearch.Text.Trim(), StringComparison.CurrentCultureIgnoreCase)));
    private async void RefreshLibrary(object sender, RoutedEventArgs args)
    {
        if (folder == null) { ChooseFolder(sender, args); return; }
        try { await LoadFolder(folder); } catch (Exception error) { Report(error); }
    }
    private void TrackClicked(object sender, ItemClickEventArgs args)
    {
        if (args.ClickedItem is LibraryTrack track) { try { Play(track); } catch (Exception error) { Report(error); } }
    }
    private void Play(LibraryTrack track)
    {
        if (!ready) return;
        Engine.Check(Engine.GA_Load(player, track.Path));
        if (current != null) current.IsCurrent = false;
        current = track; current.IsCurrent = true;
        DockTitle.Text = PlayerTitle.Text = track.Title;
        PlayerDetail.Text = track.Detail;
        Engine.Check(Engine.GA_Volume(player, verifying ? 0 : Volume.Value / 100));
        UpdateVideoHost();
        Engine.Check(Engine.GA_Play(player));
        Status.Text = "Opening local media…";
    }

    private void OnRootSizeChanged(object sender, SizeChangedEventArgs args)
    {
        if (NavigationWidth == null) return;
        NavigationWidth.Width = new GridLength(fullscreen || args.NewSize.Width < 900 ? 0 : 172);
        Navigation.Visibility = fullscreen || args.NewSize.Width < 900 ? Visibility.Collapsed : Visibility.Visible;
        CompactNavigation.Visibility = args.NewSize.Width < 900 ? Visibility.Visible : Visibility.Collapsed;
        // The timeline has its own row; it can never be pushed off-screen by the queue.
        UpdatePlayerLayout();
        UpdateGraphActivity();
        UpdateVideoHost();
    }
    private void UpdateGraphActivity()
    {
        bool minimized = AppWindow.Presenter is OverlappedPresenter presenter && presenter.State == OverlappedPresenterState.Minimized;
        GraphView.SetActive(ready && !disposed && nodesPage && mini == null && AppWindow.IsVisible && !minimized);
    }
    private void ResizeEffective(double width, double height)
    {
        double scale = Root.XamlRoot?.RasterizationScale ?? 1;
        var work = DisplayArea.GetFromWindowId(AppWindow.Id, DisplayAreaFallback.Primary).WorkArea;
        int physicalWidth = Math.Min((int)Math.Round(width * scale), (int)(work.Width * .94));
        int physicalHeight = Math.Min((int)Math.Round(height * scale), (int)(work.Height * .94));
        AppWindow.MoveAndResize(new RectInt32(work.X + (work.Width - physicalWidth) / 2, work.Y + (work.Height - physicalHeight) / 2, physicalWidth, physicalHeight));
    }
    private static void StyleTitleBar(AppWindow window)
    {
        var background = ColorHelper.FromArgb(255, 20, 30, 35);
        var foreground = ColorHelper.FromArgb(255, 218, 230, 229);
        window.TitleBar.BackgroundColor = window.TitleBar.InactiveBackgroundColor = background;
        window.TitleBar.ForegroundColor = foreground;
        window.TitleBar.ButtonBackgroundColor = window.TitleBar.ButtonInactiveBackgroundColor = background;
        window.TitleBar.ButtonForegroundColor = foreground;
        window.TitleBar.ButtonHoverBackgroundColor = ColorHelper.FromArgb(255, 44, 65, 68);
    }
    private void PlayerLayoutChanged(object sender, SizeChangedEventArgs args)
    {
        UpdatePlayerLayout();
        UpdateVideoHost();
    }
    private void UpdatePlayerLayout()
    {
        if (PlayerView == null || MediaWidth == null || QueueWidth == null) return;
        bool wide = !fullscreen && PlayerView.ActualWidth >= 850;
        MediaWidth.Width = new GridLength(wide ? 5 : 1, GridUnitType.Star);
        QueueWidth.Width = wide ? new GridLength(3, GridUnitType.Star) : new GridLength(0);
        PlayerView.ColumnSpacing = wide ? 20 : 0;
        Grid.SetColumn(PlayerQueue, wide ? 1 : 0);
        Grid.SetRow(PlayerQueue, wide ? 1 : 3);
        Grid.SetRowSpan(PlayerQueue, wide ? 3 : 1);
        PlayerMediaRow.Height = new GridLength(fullscreen || wide ? 1 : 2, GridUnitType.Star);
        PlayerQueueRow.Height = fullscreen || wide ? new GridLength(0) : new GridLength(1, GridUnitType.Star);
        PlayerQueueRow.MinHeight = fullscreen || wide ? 0 : 48;
    }
    private void OnStageSizeChanged(object sender, SizeChangedEventArgs args) => UpdateVideoHost();
    private bool UpdateVideoHost()
    {
        if (!ready || disposed) return false;
        try
        {
            bool positioned = mini != null && miniStage != null
                ? PositionHost(mini, miniStage, miniHost)
                : PositionHost(this, playerPage ? PlayerStage : DockStage, host);
            if (positioned) lastVideoHostError = null;
            return positioned;
        }
        catch (Exception error)
        {
            string signature = $"{error.GetType().FullName}:{error.HResult}:{error.Message}";
            if (lastVideoHostError != signature) { lastVideoHostError = signature; Report(error); }
            return false;
        }
    }
    private bool PositionHost(Window window, FrameworkElement stage, nint target)
    {
        if (target == 0 || stage.XamlRoot == null || stage.ActualWidth < 1 || stage.ActualHeight < 1) return false;
        var origin = stage.TransformToVisual(window.Content as UIElement).TransformPoint(new Windows.Foundation.Point());
        double scale = stage.XamlRoot.RasterizationScale;
        var bounds = (Host: target, X: (int)Math.Round(origin.X * scale), Y: (int)Math.Round(origin.Y * scale),
            Width: Math.Max(1, (int)Math.Round(stage.ActualWidth * scale)), Height: Math.Max(1, (int)Math.Round(stage.ActualHeight * scale)), Visible: current != null ? 1 : 0);
        if (lastHostBounds == bounds && attachedHost == target) return true;
        Engine.Check(Engine.GA_SetHostBounds(target, bounds.X, bounds.Y, bounds.Width, bounds.Height, bounds.Visible));
        // Attach also fits the existing native video child after a real stage resize.
        Engine.Check(Engine.GA_Attach(player, target));
        attachedHost = target;
        lastHostBounds = bounds;
        return true;
    }

    private void ShowLibrary(object sender, RoutedEventArgs args) => SetPage(false, false);
    private void ShowNowPlaying(object sender, RoutedEventArgs args) => SetPage(true, false);
    private void ShowSettings(object sender, RoutedEventArgs args) => SetPage(false, true);
    private void ShowNodes(object sender, RoutedEventArgs args)
    {
        SetPage(false, false);
        LibraryView.Visibility = Visibility.Collapsed;
        GraphView.Visibility = Visibility.Visible;
        nodesPage = true;
        SelectNavigation(NodesNav);
        UpdateGraphActivity();
    }
    private void OpenFromDock(object sender, PointerRoutedEventArgs args) => SetPage(true, false);
    private void SetPage(bool nowPlaying, bool settings)
    {
        playerPage = nowPlaying;
        nodesPage = false;
        LibraryView.Visibility = nowPlaying || settings ? Visibility.Collapsed : Visibility.Visible;
        PlayerView.Visibility = nowPlaying ? Visibility.Visible : Visibility.Collapsed;
        SettingsView.Visibility = settings ? Visibility.Visible : Visibility.Collapsed;
        GraphView.Visibility = Visibility.Collapsed;
        GraphView.SetActive(false);
        SelectNavigation(nowPlaying ? PlayingNav : settings ? SettingsNav : LibraryNav);
        DockStage.Visibility = nowPlaying ? Visibility.Collapsed : Visibility.Visible;
        DockWidth.Width = nowPlaying ? new GridLength(0) : new GridLength(128);
        Root.UpdateLayout();
        UpdatePlayerLayout();
        UpdateVideoHost();
    }
    private void SelectNavigation(Button selected)
    {
        foreach (var item in new[] { LibraryNav, NodesNav, PlayingNav, SettingsNav })
            item.Background = new SolidColorBrush(item == selected ? ColorHelper.FromArgb(255, 38, 61, 59) : Colors.Transparent);
    }
    private void FocusQueue(object sender, RoutedEventArgs args)
    {
        QueueSearch.Visibility = QueueSearch.Visibility == Visibility.Visible ? Visibility.Collapsed : Visibility.Visible;
        if (QueueSearch.Visibility == Visibility.Visible) QueueSearch.Focus(FocusState.Programmatic);
    }
    private void PlayPause(object sender, RoutedEventArgs args)
    {
        if (!ready || current == null) return;
        try
        {
            Engine.Check(Engine.GA_GetState(player, out var live));
            Engine.Check(live.State == 3 ? Engine.GA_Pause(player, 1) : Engine.GA_Play(player));
            Tick();
        }
        catch (Exception error) { Report(error); }
    }
    private void ToggleRepeat(object sender, RoutedEventArgs args)
    {
        repeat = !repeat;
        RepeatIcon.Opacity = repeat ? 1 : .4;
        ToolTipService.SetToolTip((Button)sender, repeat ? "Repeat this track" : "Repeat off");
    }
    private void Previous(object sender, RoutedEventArgs args) => Step(-1);
    private void Next(object sender, RoutedEventArgs args) => Step(1);
    private void Step(int direction)
    {
        if (library.Count == 0) return;
        int index = current == null ? 0 : library.FindIndex(track => track.Path == current.Path);
        try { Play(library[(index + direction + library.Count) % library.Count]); } catch (Exception error) { Report(error); }
    }
    private void SeekChanged(object sender, RangeBaseValueChangedEventArgs args)
    {
        if (!ready || updatingTimeline || scrubbing || current == null) return;
        try { Engine.Check(Engine.GA_Seek(player, args.NewValue)); }
        catch (Exception error) { Report(error); }
    }
    private void EndScrub()
    {
        if (!scrubbing) return;
        scrubbing = false;
        if (!ready || current == null) return;
        try { Engine.Check(Engine.GA_Seek(player, Timeline.Value)); }
        catch (Exception error) { Report(error); }
    }
    private void SetSpeed(object sender, RoutedEventArgs args)
    {
        if (sender is not MenuFlyoutItem item || !double.TryParse(item.Tag?.ToString(), NumberStyles.Float, CultureInfo.InvariantCulture, out double speed) || !ready) return;
        try
        {
            Engine.Check(Engine.GA_Rate(player, speed));
            SpeedButton.Content = $"{speed:0.0#}×";
        }
        catch (Exception error) { Report(error); }
    }
    private void VolumeChanged(object sender, RangeBaseValueChangedEventArgs args)
    {
        if (!ready || verifying) return;
        try { Engine.Check(Engine.GA_Volume(player, args.NewValue / 100)); }
        catch (Exception error) { Report(error); }
    }
    private void AmbientChanged(object sender, RoutedEventArgs args)
    {
        if (!ready) return;
        try
        {
            Engine.Check(Engine.GA_AmbientEnable(player, Ambient.IsOn ? 1 : 0));
            lastPaletteSample = 0;
            if (!Ambient.IsOn) Root.Background = new SolidColorBrush(ColorHelper.FromArgb(255, 16, 23, 27));
            if (!verifying)
            {
                Directory.CreateDirectory(App.DataFolder);
                File.WriteAllText(Path.Combine(App.DataFolder, "ambient.txt"), Ambient.IsOn ? "on" : "off");
            }
        }
        catch (Exception error) { Report(error); }
    }
    private void UpdateAmbient()
    {
        if (!Ambient.IsOn || state.PaletteSamples == 0 || state.PaletteSamples == lastPaletteSample) return;
        lastPaletteSample = state.PaletteSamples;
        // The engine exposes Windows COLORREF (BBGGRR), not XAML ARGB.
        var brush = new LinearGradientBrush { StartPoint = new Windows.Foundation.Point(0, 0), EndPoint = new Windows.Foundation.Point(1, 1) };
        brush.GradientStops.Add(new GradientStop { Color = Tint(state.Palette0), Offset = 0 });
        brush.GradientStops.Add(new GradientStop { Color = Tint(state.Palette1), Offset = 1 });
        Root.Background = brush;
        static Windows.UI.Color Tint(uint color) => ColorHelper.FromArgb(255,
            (byte)(16 + (color & 255) * .16), (byte)(23 + ((color >> 8) & 255) * .16), (byte)(27 + ((color >> 16) & 255) * .16));
    }
    private void OnKeyDown(object sender, KeyRoutedEventArgs args)
    {
        if (args.OriginalSource is TextBox or Button or Slider or ToggleSwitch) return;
        if (args.Key == Windows.System.VirtualKey.Space) { PlayPause(sender, new RoutedEventArgs()); args.Handled = true; }
        else if (args.Key == Windows.System.VirtualKey.Escape && fullscreen) { SetFullscreen(false); args.Handled = true; }
    }
    private void ToggleFullscreen(object sender, RoutedEventArgs args) => SetFullscreen(!fullscreen);
    private void SetFullscreen(bool value)
    {
        SetPage(true, false);
        fullscreen = value;
        BrandHeader.Visibility = value ? Visibility.Collapsed : Visibility.Visible;
        PlayerIdentity.Visibility = PlayerQueue.Visibility = value ? Visibility.Collapsed : Visibility.Visible;
        UpdatePlayerLayout();
        PlayerView.Padding = value ? new Thickness(0) : new Thickness(24, 16, 24, 16);
        PlayerView.RowSpacing = value ? 0 : 12;
        Navigation.Visibility = value ? Visibility.Collapsed : (Root.ActualWidth >= 900 ? Visibility.Visible : Visibility.Collapsed);
        NavigationWidth.Width = new GridLength(value || Root.ActualWidth < 900 ? 0 : 172);
        AppWindow.SetPresenter(value ? AppWindowPresenterKind.FullScreen : AppWindowPresenterKind.Overlapped);
        Root.UpdateLayout(); UpdateVideoHost();
    }

    private void Detach(object sender, RoutedEventArgs args) { try { OpenMini(); } catch (Exception error) { Report(error); } }
    private void OpenMini()
    {
        if (mini != null || !ready || current == null) return;
        if (fullscreen) SetFullscreen(false);
        mini = new Window { Title = "Greater Art · Mini" };
        StyleTitleBar(mini.AppWindow);
        var icon = Path.Combine(AppContext.BaseDirectory, "Assets", "app.ico");
        if (File.Exists(icon)) mini.AppWindow.SetIcon(icon);
        var root = new Grid { Background = new SolidColorBrush(ColorHelper.FromArgb(255, 16, 23, 27)), RequestedTheme = ElementTheme.Dark };
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
        root.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        miniStage = new Border { Background = new SolidColorBrush(Colors.Black) };
        root.Children.Add(miniStage);
        var controls = new Grid { Padding = new Thickness(8), ColumnSpacing = 8 };
        controls.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        controls.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        var expand = new Button { Content = new TextBlock { Text = current.Title, TextTrimming = TextTrimming.CharacterEllipsis }, HorizontalAlignment = HorizontalAlignment.Stretch, HorizontalContentAlignment = HorizontalAlignment.Left };
        expand.Click += (_, _) => RestoreFromMini();
        miniPlayIcon = new FontIcon { Glyph = state.State == 3 ? "\uE769" : "\uE768", FontSize = 22 };
        var pause = new Button { Content = miniPlayIcon, Width = 44, Height = 44 };
        Microsoft.UI.Xaml.Automation.AutomationProperties.SetName(pause, "Play or pause");
        pause.Click += PlayPause;
        Grid.SetColumn(pause, 1); controls.Children.Add(expand); controls.Children.Add(pause);
        Grid.SetRow(controls, 1); root.Children.Add(controls);
        mini.Content = root;
        mini.AppWindow.Resize(new SizeInt32(420, 300));
        miniHost = Engine.GA_CreateHost(WinRT.Interop.WindowNative.GetWindowHandle(mini));
        if (miniHost == 0)
        {
            mini.Close(); mini = null; miniStage = null; miniPlayIcon = null;
            throw new InvalidOperationException("The detached video surface could not be created.");
        }
        root.LayoutUpdated += (_, _) => { if (mini != null) UpdateVideoHost(); };
        mini.AppWindow.Closing += (_, args) =>
        {
            if (disposed || mini == null) return;
            // Move the video child before Windows destroys the Mini's HWND tree.
            args.Cancel = true;
            RestoreFromMini();
        };
        mini.Activate();
        root.UpdateLayout();
        if (UpdateVideoHost()) { GraphView.SetActive(false); AppWindow.Hide(); }
    }
    private void RestoreFromMini(bool close = true)
    {
        var old = mini;
        if (old == null) return;
        var oldStage = miniStage;
        var oldIcon = miniPlayIcon;
        mini = null; miniStage = null; miniPlayIcon = null;
        SetPage(true, false);
        AppWindow.Show();
        Activate();
        Root.UpdateLayout();
        if (!UpdateVideoHost())
        {
            mini = old; miniStage = oldStage; miniPlayIcon = oldIcon;
            return;
        }
        if (miniHost != 0) { Engine.GA_DestroyHost(miniHost); miniHost = 0; }
        if (close) old.Close();
    }

    private void Tick()
    {
        if (!ready || disposed) return;
        try
        {
            Engine.Check(Engine.GA_GetState(player, out state));
            if (current == null) return;
            updatingTimeline = true;
            Timeline.Maximum = Math.Max(1, state.Duration);
            if (!scrubbing) Timeline.Value = Math.Clamp(state.Position, 0, Timeline.Maximum);
            updatingTimeline = false;
            Elapsed.Text = FormatTime(state.Position); Duration.Text = state.Duration > 0 ? FormatTime(state.Duration) : "—:—";
            PlayIcon.Glyph = state.State == 3 ? "\uE769" : "\uE768";
            if (miniPlayIcon != null) miniPlayIcon.Glyph = PlayIcon.Glyph;
            UpdateAmbient();
            Status.Text = state.State switch { 1 => "Preparing local media…", 3 => state.Width > 0 ? $"{state.Width} × {state.Height} · original video" : "Playing local audio", 4 => "Paused", 7 => $"Playback error {state.VideoError}", _ => "Local playback" };
            if (state.State == 6 && !verifying) { if (repeat) { Engine.Check(Engine.GA_Seek(player, 0)); Engine.Check(Engine.GA_Play(player)); } else Step(1); }
        }
        catch (Exception error) { timer.Stop(); Report(error); }
    }
    private static string FormatTime(double seconds)
    {
        var time = TimeSpan.FromSeconds(Math.Max(0, double.IsFinite(seconds) ? seconds : 0));
        return time.TotalHours >= 1 ? $"{(int)time.TotalHours}:{time.Minutes:00}:{time.Seconds:00}" : $"{(int)time.TotalMinutes}:{time.Seconds:00}";
    }
    private void Report(Exception error) { App.RecordFailure(error); if (Status != null) Status.Text = error.Message; }
    private void DisposePlayback()
    {
        if (disposed) return;
        disposed = true; ready = false; timer.Stop();
        GraphView.Dispose();
        // The renderer must leave its HWND parents before Mini or the main host is destroyed.
        if (player != 0) { Engine.GA_Release(player); player = 0; }
        mini?.Close(); mini = null;
        if (miniHost != 0) Engine.GA_DestroyHost(miniHost);
        if (host != 0) Engine.GA_DestroyHost(host);
        Engine.GA_Shutdown();
    }

    private async Task VerifyFlow(string path, string directory)
    {
        var checks = new List<object>();
        bool passed = false;
        Directory.CreateDirectory(directory);
        try
        {
            if (!File.Exists(path)) throw new FileNotFoundException("Owned verification video is missing.");
            var track = new LibraryTrack(Path.GetFullPath(path));
            library.Add(track); visibleTracks.Add(track); queue.Add(track);
            Engine.Check(Engine.GA_Volume(player, 0));
            Play(track);
            await WaitFor(() => Read().State == 3 && Read().Frames > 4 && Read().Width > 0, "dock first frames");
            var initial = Read();
            Check(!playerPage, "library selection keeps library open");
            checks.Add(new { check = "library-dock", state = Snapshot(initial) });
            Capture(directory, "library");
            Invoke(DockOpenButton);
            await WaitFor(() => playerPage, "dock button opens Now Playing");
            await Task.Delay(600);
            var expanded = Read();
            Check(expanded.Identity == initial.Identity && expanded.Position >= initial.Position - .25 && expanded.Frames > initial.Frames, "expanded retains player and advances frames");
            Ambient.IsOn = true;
            await WaitFor(() => Read().PaletteSamples > 0, "live video ambient samples");
            Check(Read().Width == initial.Width && Read().Height == initial.Height, "Ambient does not change source resolution");
            checks.Add(new { check = "expanded", state = Snapshot(expanded) }); Capture(directory, "expanded");
            double seekTarget = Math.Min(8, initial.Duration / 3);
            Engine.Check(Engine.GA_Seek(player, seekTarget));
            await WaitFor(() => Read().State == 3 && Math.Abs(Read().Position - seekTarget) < 1, "seek reaches requested media time");
            Invoke(PlayButton);
            await WaitFor(() => Read().State == 4, "pause button changes playback state");
            await Task.Delay(200);
            var paused = Read(); await Task.Delay(250);
            Check(Read().State == 4 && Math.Abs(Read().Position - paused.Position) < .15, "pause remains paused");
            Invoke(PlayButton);
            await Task.Delay(700);
            var resumed = Read();
            Check(resumed.State == 3 && resumed.Position > paused.Position + .3 && resumed.Frames > paused.Frames, "resume advances media time and video frames");
            checks.Add(new { check = "seek-pause-resume", requestedSeconds = seekTarget, paused = Snapshot(paused), resumed = Snapshot(resumed) });
            Invoke(MiniButton); await Task.Delay(700);
            var small = Read();
            Check(attachedHost == miniHost && miniHost != 0, "Mini owns the active native video host");
            Check(small.Identity == initial.Identity && small.Width == initial.Width && small.Height == initial.Height && small.Frames > expanded.Frames, "mini retains player and original video");
            checks.Add(new { check = "mini", state = Snapshot(small) }); Capture(directory, "mini");
            RestoreFromMini(); await Task.Delay(400);
            Invoke(FullscreenButton); await Task.Delay(700);
            var full = Read();
            Check(AppWindow.Presenter.Kind == AppWindowPresenterKind.FullScreen && full.Identity == initial.Identity && full.Frames > small.Frames, "real fullscreen retains player");
            checks.Add(new { check = "fullscreen", state = Snapshot(full) }); Capture(directory, "fullscreen");
            SetFullscreen(false); SetPage(false, false); await Task.Delay(350);
            var returned = Read();
            Check(returned.Identity == initial.Identity && returned.Width == initial.Width && returned.Height == initial.Height, "return to dock retains source resolution");
            Engine.Check(Engine.GA_Rate(player, 1.25));
            await Task.Delay(400);
            double rateStart = Read().Position;
            var rateClock = System.Diagnostics.Stopwatch.StartNew();
            await Task.Delay(1600);
            double measuredRate = (Read().Position - rateStart) / rateClock.Elapsed.TotalSeconds;
            Check(measuredRate > 1.08 && measuredRate < 1.42, "1.25× speed changes actual media-time progress");
            Engine.Check(Engine.GA_Rate(player, 1));
            Engine.Check(Engine.GA_Seek(player, 1));
            await WaitFor(() => Read().State == 3 && Read().Position >= .8 && Read().Position < 2, "backward seek reaches requested media time");
            checks.Add(new { check = "dock-return-speed", measuredRate, state = Snapshot(Read()) });
            ResizeEffective(800, 600); SetPage(true, false);
            await Task.Delay(600);
            foreach (var control in new FrameworkElement[] { RepeatButton, PreviousButton, PlayButton, NextButton, SpeedButton, Timeline })
            {
                var corner = control.TransformToVisual(Root).TransformPoint(new Windows.Foundation.Point());
                Check(control.ActualWidth > 0 && control.ActualHeight > 0 && corner.X >= -.5 && corner.Y >= -.5
                    && corner.X + control.ActualWidth <= Root.ActualWidth + .5 && corner.Y + control.ActualHeight <= Root.ActualHeight + .5,
                    $"compact primary control remains within client: {control.Name}");
            }
            checks.Add(new { check = "compact-fixed-controls", width = Root.ActualWidth, height = Root.ActualHeight, state = Snapshot(Read()) });
            Capture(directory, "compact");
            ResizeEffective(1180, 820);
            for (int cycle = 0; cycle < 2; cycle++)
            {
                var beforeCycle = Read();
                Invoke(MiniButton); await Task.Delay(500);
                Check(attachedHost == miniHost && miniHost != 0 && Read().Identity == initial.Identity, "repeated Mini transfer retains owner and player");
                RestoreFromMini(); await Task.Delay(300);
                Invoke(FullscreenButton); await Task.Delay(400);
                Check(AppWindow.Presenter.Kind == AppWindowPresenterKind.FullScreen && Read().Identity == initial.Identity && Read().Frames > beforeCycle.Frames, "repeated fullscreen transfer advances frames");
                Invoke(FullscreenButton); await Task.Delay(250);
                checks.Add(new { check = "repeat-presentation-cycle", cycle = cycle + 1, state = Snapshot(Read()) });
            }
            var beforeGraph = Read();
            await LoadFolder(Path.GetDirectoryName(track.Path)!);
            Invoke(NodesNav);
            await WaitFor(() => GraphView.IsReady && GraphView.NodeCount == library.Count && GraphView.HasFiniteGeometry, "real-library Nodes graph is finite and complete");
            await Task.Delay(1000);
            var graphChecks = GraphPage.Verify();
            Check(graphChecks.All(result => result.Value), "Nodes transform and ABI checks pass");
            var afterGraph = Read();
            Check(afterGraph.Identity == initial.Identity && afterGraph.Frames > beforeGraph.Frames, "Nodes keeps the original player advancing");
            checks.Add(new { check = "real-library-nodes", files = library.Count, nodes = GraphView.NodeCount, edges = GraphView.EdgeCount, mathAndAbi = graphChecks,
                renderCallbacks = GraphView.RenderCallbackCount, snapshots = GraphView.SnapshotCount,
                callbackAverageMs = GraphView.RenderAverageMilliseconds, callbackWorstMs = GraphView.RenderWorstMilliseconds,
                state = Snapshot(afterGraph) });
            Capture(directory, "nodes");
            SetPage(false, false);
            await Task.Delay(200);
            Check(!GraphView.IsMotionActive, "hidden Nodes unsubscribes rendering");
            checks.Add(new { check = "hidden-nodes-inactive", rendering = GraphView.IsMotionActive });
            Ambient.IsOn = false;
            passed = true;
        }
        catch (Exception error) { checks.Add(new { check = "failure", error = error.ToString() }); App.RecordFailure(error); }
        File.WriteAllText(Path.Combine(directory, "flow-result.json"), JsonSerializer.Serialize(new { passed, checks, qualityConstraints = "none", inputMechanism = "WinUI UI Automation Invoke; physical pointer hit-testing is not proved by this test.", screenshotProof = "Owned HWND BMP captures are separate from renderer counters." }, new JsonSerializerOptions { WriteIndented = true }));
        Environment.ExitCode = passed ? 0 : 2;
        DisposePlayback();
        Close();

        PlaybackState Read() { Engine.Check(Engine.GA_GetState(player, out var result)); return result; }
        static void Invoke(Button button)
        {
            var peer = FrameworkElementAutomationPeer.FromElement(button) ?? new ButtonAutomationPeer(button);
            if (peer.GetPattern(PatternInterface.Invoke) is not IInvokeProvider invoke) throw new InvalidOperationException($"Button has no Invoke pattern: {button.Name}");
            invoke.Invoke();
        }
        void Check(bool condition, string name) { if (!condition) throw new InvalidOperationException(name); }
        async Task WaitFor(Func<bool> condition, string name)
        {
            var deadline = DateTime.UtcNow + TimeSpan.FromSeconds(20);
            while (!condition()) { if (DateTime.UtcNow > deadline) throw new TimeoutException(name); await Task.Delay(100); }
        }
        void Capture(string output, string name)
        {
            Engine.Check(Engine.GA_CaptureHost(attachedHost, Path.Combine(output, name + "-video.bmp")));
            Engine.Check(Engine.GA_CaptureHost(mini != null ? WinRT.Interop.WindowNative.GetWindowHandle(mini) : WindowHandle, Path.Combine(output, name + "-window.bmp")));
        }
        static object Snapshot(PlaybackState s) => new { s.Position, s.Duration, s.State, s.Width, s.Height, s.Frames, s.Identity, s.VideoError, s.BundledAudio, s.PaletteSamples };
    }
}
