package com.local.listentomusic.playback

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.ActivityManager
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowInsets
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.media3.common.Player
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.common.util.concurrent.ListenableFuture
import com.local.listentomusic.R
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.MainActivity
import com.local.listentomusic.ui.ShareProxyActivity
import com.local.listentomusic.model.MiniWindowMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

// One floating window. Compact presentation is native Android; Compose is created
// only when the user expands it, keeping song-start on the proven lightweight path.
class MiniWindowOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner,
    SavedStateRegistryOwner, OnBackPressedDispatcherOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val onBackPressedDispatcher = OnBackPressedDispatcher { collapseExpanded() }
    private lateinit var viewModel: MainViewModel
    private var wm: WindowManager? = null
    private var root: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val isVideo = MutableStateFlow(false)
        private val videoExtensions = setOf("mp4", "mov", "m4v", "mkv", "webm", "3gp", "ts", "mpeg", "mpg", "flv", "avi")

        // The visible target sits immediately above the real navigation-bar inset.
        // Visible circle and collision radius are identical. With BOTTOM gravity, larger y is higher.
        private val crossHitSize = 57
                private val crossSize = 25
                private val crossMargin = 9
                private val crossBaseAlpha = 1f
                private val crossRaisePx = 28
        private var crossActive: Boolean? = null
        private var framePending = false
        private var openingApp = false
    private var gestureGeneration = 0
    private val dragFrame = Runnable {
        framePending = false
        updateRootLayout()
        root?.post { if (dragging) updateCrossAppearance(miniOverlapsCross()) }
    }

    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false
    private var mode = PlayerWindowMode.DETACHED
    private val modeState = MutableStateFlow(PlayerWindowMode.DETACHED)
    private val docked get() = mode == PlayerWindowMode.DOCKED
    private val expanded get() = mode == PlayerWindowMode.EXPANDED
    private var expandedView: ComposeView? = null
    private var expandedHostReady = false
    private var shareReceiverRegistered = false
    private var homeReceiverRegistered = false
    private var expandedFullscreen = false
    private var fullscreenActivityActive = false
    private var detachUntilLibraryHidden = false
    private var windowAnimator: ValueAnimator? = null
    private var sharing = false
    private var savedWindowFlags = 0
    private val shareFinished = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ShareProxyActivity.ACTION_FINISHED) restoreAfterShare()
        }
    }
    private val systemHome = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS &&
                shouldShrinkForSystemReason(intent.getStringExtra("reason")) && expanded) {
                detachUntilLibraryHidden = true
                switchMode(PlayerWindowMode.DETACHED)
            }
        }
    }

    // views
    private var dangerTint: View? = null
    private var videoView: PlayerView? = null
    private var artworkGeneration = 0
    private var artworkAspect = 1f

    // drag-to-close drop target (red cross at screen bottom-center)
    private var crossView: FrameLayout? = null
    private var crossImg: ImageView? = null
    private var crossParams: WindowManager.LayoutParams? = null

    companion object {
        internal val destinationReady = MutableStateFlow(false)
        internal val modeSnapshot = MutableStateFlow<PlayerWindowMode?>(null)
        internal val identities = MutableStateFlow("controllerId=none playerViewId=none")
        @Volatile internal var shareInProgress = false
        // Reuse the media channel so Android accepts the foreground promotion.
        private const val CHANNEL_ID = "greater_art_playback"
        const val EXTRA_OPEN_PLAYER = "open_player"
        const val ACTION_DOCK = "com.local.listentomusic.player.DOCK"
        const val ACTION_DETACH = "com.local.listentomusic.player.DETACH"
        const val ACTION_EXPAND = "com.local.listentomusic.player.EXPAND"
        const val ACTION_FULLSCREEN_RETURN = "com.local.listentomusic.player.FULLSCREEN_RETURN"
        const val EXTRA_FULLSCREEN_DESTINATION = "fullscreen_destination"
        private const val POSITION_PREFS = "mini_window_position"
        private const val POSITION_X = "x"
        private const val POSITION_Y = "y"
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        destinationReady.value = false
        com.local.listentomusic.ui.components.VideoSurfaceOwner.serviceEvent("start")
        com.local.listentomusic.ui.components.VideoSurfaceOwner.setSystemOverlayVisible(
            "mini_window_overlay",
            "MINI_WINDOW",
            true,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        createChannel()
                mode = if (PlayerWindowVisibility.libraryShowing.value) PlayerWindowMode.DOCKED else PlayerWindowMode.DETACHED
                modeState.value = mode
                modeSnapshot.value = mode
                startForeground(2, buildNotification())
                wm = getSystemService(WindowManager::class.java)
                buildView()
                buildCross()
                params = WindowManager.LayoutParams(
                    miniWidthPx(), miniHeightPx(),
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.RGBA_8888,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            alpha = 0f
            if (alpha == 0f) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            if (Build.VERSION.SDK_INT >= 30) {
                // Protect the status bar, but deliberately allow the user to drag over
                // the navigation-bar area just like the older mini window.
                setFitInsetsTypes(WindowInsets.Type.statusBars())
                setFitInsetsSides(WindowInsets.Side.TOP)
            }
            val saved = getSharedPreferences(POSITION_PREFS, MODE_PRIVATE)
            x = saved.getInt(POSITION_X, dp(12))
            y = saved.getInt(POSITION_Y, dp(300))
        }
        applyModeLayout()
        try {
            root?.let { wm?.addView(it, params!!) }
        } catch (t: Throwable) {
            // Keep the user's Mini preference intact. A temporary OEM overlay failure
            // must not silently rewrite Settings to a different floating mode.
            stopSelf()
            return
        }
        connect()
        scope.launch {
            com.local.listentomusic.data.AppPreferences(applicationContext).values.collect {
                compact?.appearance(it)
            }
        }
        scope.launch {
            combine(PlayerWindowVisibility.libraryShowing, PlayerWindowVisibility.detachedVisible,
                PlayerWindowVisibility.dockedVisible) { library, detached, _ -> library to detached }
                .collect { (library, detached) ->
                if (!library) detachUntilLibraryHidden = false
                if (!expanded && docked != library && (library || detached) &&
                    !(library && detachUntilLibraryHidden)) switchMode(library)
                updateVisibility()
            }
        }
        val touch = View.OnTouchListener { view, event -> drag(view, event) }
        root?.setOnTouchListener(touch)
        root?.setOnClickListener { openApp() }

        // Coroutine-driven view updates are guarded because destruction can race collection.
        scope.launch {
            isVideo.collect { v ->
                updateMiniWindowSize()
                clampPosition()
                updateRootLayout()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_FULLSCREEN_RETURN) {
            com.local.listentomusic.ui.components.VideoSurfaceOwner.beginHandoff("MINI_WINDOW")
            fullscreenActivityActive = false
            val target = when (intent.getStringExtra(EXTRA_FULLSCREEN_DESTINATION)) {
                "DOCKED" -> PlayerWindowMode.DOCKED
                "DETACHED" -> PlayerWindowMode.DETACHED
                else -> PlayerWindowMode.EXPANDED
            }
            switchMode(target)
            updateVisibility()
            com.local.listentomusic.ui.components.VideoSurfaceOwner.finishHandoff("MINI_WINDOW")
            return START_NOT_STICKY
        }
        val wasDocked = docked
        when (intent?.action) {
            ACTION_DOCK -> { detachUntilLibraryHidden = false; switchMode(true) }
            ACTION_DETACH -> {
                detachUntilLibraryHidden = true
                switchMode(false)
            }
            ACTION_EXPAND -> switchMode(PlayerWindowMode.EXPANDED)
        }
        val handoffNeeded = controller == null ||
            com.local.listentomusic.ui.components.VideoSurfaceOwner.expectedOwner != "MINI_WINDOW"
        if (handoffNeeded) {
            com.local.listentomusic.ui.components.VideoSurfaceOwner.beginHandoff("MINI_WINDOW")
        }
        if (handoffNeeded) controller?.let { push(it); awaitDestinationReady() }
        else if (controller != null && !wasDocked && docked && ready) updateVisibility()
        return START_NOT_STICKY
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mini window active")
            .setContentText("Tap the floating player to open Greater Art")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

    private fun createChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Mini window", NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) },
        )
    }

    private fun openApp() {
        if (openingApp) return
        openingApp = true
        switchMode(PlayerWindowMode.EXPANDED)
        openingApp = false
    }

    // Dragging onto the center red cross closes the window and stops playback.
    private fun closeAndStopApp() {
        ParallelPlayback.stopAll()
        // Stop media first. Calling stopSelf before this can race onDestroy and release
        // the controller before playback receives the stop command.
        controller?.run {
            stop()
            clearMediaItems()
        }
        stopService(Intent(this, PlaybackService::class.java))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        // Removing the existing task never launches Library as an intermediate screen.
        runCatching {
            getSystemService(ActivityManager::class.java).appTasks.forEach { it.finishAndRemoveTask() }
        }
    }

    private var compact: com.local.listentomusic.ui.components.CompactPlayerView? = null
    private var connectionLease: SharedPlaybackResource.Lease<ListenableFuture<MediaController>>? = null
    private var ready = false

    private fun buildView() {
        root = object : FrameLayout(this) {
            override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
                if (docked || expanded) return false
                if (event.actionMasked == MotionEvent.ACTION_DOWN) drag(this, event)
                if (event.actionMasked == MotionEvent.ACTION_MOVE &&
                    (abs(event.rawX - downX) > android.view.ViewConfiguration.get(context).scaledTouchSlop ||
                     abs(event.rawY - downY) > android.view.ViewConfiguration.get(context).scaledTouchSlop)) return true
                return false
            }
        }
        // Compose installs its window recomposer on the window root, not on the
        // nested ComposeView. All owner tags must exist here before attachment.
        root!!.setViewTreeLifecycleOwner(this)
        root!!.setViewTreeViewModelStoreOwner(this)
        root!!.setViewTreeSavedStateRegistryOwner(this)
        root!!.setViewTreeOnBackPressedDispatcherOwner(this)
        compact = com.local.listentomusic.ui.components.CompactPlayerView(this).also {
            it.onOpen = { openApp() }
            root!!.addView(it, FrameLayout.LayoutParams(-1, -1))
            videoView = it.video
            it.setDetached(!docked)
        }
        dangerTint = View(this).apply {
            setBackgroundColor(0x66FF3B30)
            visibility = View.GONE
            isClickable = false
        }
        root!!.addView(dangerTint, FrameLayout.LayoutParams(-1, -1))
    }

    private fun ensureExpandedHost(): Boolean {
        if (expandedHostReady) return true
        return runCatching {
            savedStateController.performAttach()
            savedStateController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            viewModel = ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(application))[MainViewModel::class.java]
            viewModel.useSessionPresentationOnly()
            ContextCompat.registerReceiver(this, shareFinished,
                IntentFilter(ShareProxyActivity.ACTION_FINISHED), ContextCompat.RECEIVER_NOT_EXPORTED)
            shareReceiverRegistered = true
            runCatching {
                ContextCompat.registerReceiver(this, systemHome,
                    IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS), ContextCompat.RECEIVER_NOT_EXPORTED)
                homeReceiverRegistered = true
            }
            expandedView = ComposeView(this).apply {
            visibility = View.GONE
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(this@MiniWindowOverlayService)
            setViewTreeViewModelStoreOwner(this@MiniWindowOverlayService)
            setViewTreeSavedStateRegistryOwner(this@MiniWindowOverlayService)
            setViewTreeOnBackPressedDispatcherOwner(this@MiniWindowOverlayService)
            setContent {
                val currentMode by modeState.collectAsState()
                if (currentMode == PlayerWindowMode.EXPANDED) {
                    PlayerWindowExpandedContent(
                        viewModel = viewModel,
                        videoView = videoView,
                        scope = scope,
                        onVideoReleased = { compact?.restoreVideo() },
                        onHome = ::returnToLibrary,
                        onClose = ::collapseExpanded,
                        onShrink = {
                            sendBroadcast(Intent(MainActivity.ACTION_BACKGROUND_PLAYER).setPackage(packageName))
                            switchMode(PlayerWindowMode.DETACHED)
                        },
                        onFullscreen = { value ->
                            if (value && viewModel.playback.value.isVideo) openLandscapeFullscreen()
                            else updateExpandedFullscreen(value)
                        },
                        onPull = ::dragExpanded,
                        onPullEnd = ::finishExpandedPull,
                        onPullCancel = ::resetExpandedPull,
                        onShare = ::launchShare,
                        onShareFailure = { Toast.makeText(this@MiniWindowOverlayService, it, Toast.LENGTH_LONG).show() },
                    )
                }
            }
            }.also { root!!.addView(it, FrameLayout.LayoutParams(-1, -1)) }
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            expandedHostReady = true
            true
        }.getOrElse { error ->
            android.util.Log.e("PlayerWindow", "Expanded player unavailable", error)
            Toast.makeText(this, "Could not open floating player", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun buildCross() {
            crossView = FrameLayout(this).apply {
                // This circle is the real hit area—not decoration with a different size.
                // Its faint fill makes the exact quit zone visible without shouting.
                background = crossTargetDrawable(active = false)
            }
            crossImg = ImageView(this).apply {
                setImageResource(R.drawable.ic_red_cross)
                setColorFilter(0xFFFFFFFF.toInt(), android.graphics.PorterDuff.Mode.SRC_IN)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
            crossView?.addView(crossImg!!, FrameLayout.LayoutParams(dp(crossSize), dp(crossSize)).apply { gravity = Gravity.CENTER })
            crossView?.visibility = View.INVISIBLE
        val layout = WindowManager.LayoutParams(
            dp(crossHitSize), dp(crossHitSize),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.RGBA_8888,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            // Overlay bounds are already inset from system bars on affected Samsung builds.
            // Adding the navigation inset again placed the X too high and broke collision.
            y = dp(crossMargin) + crossRaisePx
        }
        crossParams = layout
        try {
            crossView?.let { wm?.addView(it, layout) }
        } catch (t: Throwable) {
            // Some OEMs reject a second overlay window. The player is still useful
            // without the drag-to-close target, so keep it alive.
            crossView = null
            crossImg = null
        }
    }

    private fun connect() {
        val lease = PlaybackConnection.acquire(this)
        connectionLease = lease
        val pending = lease.value
        future = pending
        pending.addListener({
            if (future !== pending) {
                return@addListener
            }
            runCatching { pending.get() }.onSuccess { c ->
                controller = c
                identities.value = "controllerId=${System.identityHashCode(c)} playerViewId=${System.identityHashCode(videoView)}"
                c.addListener(listener)
                push(c)
                awaitDestinationReady()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private var readinessJob: kotlinx.coroutines.Job? = null
    private val sessionHasMedia = MutableStateFlow(false)

    private fun awaitDestinationReady() {
        readinessJob?.cancel()
        ready = false
        destinationReady.value = false
        params?.let {
            it.alpha = 0f
            it.flags = it.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        updateRootLayout()
        readinessJob = scope.launch {
            // This is the only player window. Show its surface while the decoder
            // warms up; closing it before the first frame prevents that frame.
            sessionHasMedia.first { it }
            ready = true
            destinationReady.value = true
            params?.let {
                updateVisibility()
            }
            updateRootLayout()
            com.local.listentomusic.ui.components.VideoSurfaceOwner.finishHandoff("MINI_WINDOW")
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            push(
                player,
                artworkMayHaveChanged = events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                    events.contains(Player.EVENT_MEDIA_METADATA_CHANGED),
                layoutMayHaveChanged = events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                    events.contains(Player.EVENT_MEDIA_METADATA_CHANGED) ||
                    events.contains(Player.EVENT_VIDEO_SIZE_CHANGED),
            )
        }
    }

    private fun push(
        p: Player,
        artworkMayHaveChanged: Boolean = true,
        layoutMayHaveChanged: Boolean = true,
    ) {
        compact?.bind(p, "MINI_WINDOW")
        val path = p.currentMediaItem?.mediaId
        val videoNow = p.currentMediaItem?.mediaMetadata?.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO ||
            path?.substringAfterLast('.').orEmpty().lowercase() in videoExtensions
        val videoChanged = isVideo.value != videoNow
        if (videoChanged) isVideo.value = videoNow
        if (layoutMayHaveChanged && !videoChanged) updateMiniWindowSize()
        if (artworkMayHaveChanged) updateArtwork(p.mediaMetadata.artworkData)
        sessionHasMedia.value = p.currentMediaItem != null
    }

    private fun drag(view: View, event: MotionEvent): Boolean {
        if (docked || expanded) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gestureGeneration++
                downX = event.rawX
                downY = event.rawY
                startX = params?.x ?: 0
                startY = params?.y ?: 0
                dragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                val slop = android.view.ViewConfiguration.get(this).scaledTouchSlop
                if (!dragging && (abs(dx) > slop || abs(dy) > slop)) {
                    dragging = true
                    updateCrossAppearance(false)
                    crossView?.visibility = View.VISIBLE
                }
                if (dragging) {
                    params?.x = startX + dx.toInt()
                    params?.y = startY + dy.toInt()
                    clampPosition()
                    if (!framePending) {
                        framePending = true
                        root?.postOnAnimation(dragFrame)
                    }
                    return true
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val was = dragging
                dragging = false
                if (was) {
                    root?.removeCallbacks(dragFrame)
                    framePending = false
                    updateRootLayout()
                    savePosition()
                    // Read actual on-screen coordinates after the final layout, not
                    // the previous move event's position.
                    val generation = gestureGeneration
                    root?.postOnAnimation {
                        root?.postOnAnimation {
                            if (generation == gestureGeneration) {
                                val overlap = miniOverlapsCross()
                                updateCrossAppearance(false)
                                crossView?.visibility = View.INVISIBLE
                                if (overlap) closeAndStopApp()
                            }
                        }
                    }
                    return true
                }
                view.performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                gestureGeneration++
                root?.removeCallbacks(dragFrame)
                framePending = false
                dragging = false
                updateCrossAppearance(false)
                crossView?.visibility = View.INVISIBLE
                return true
            }
        }
        return false
    }

    private fun updateCrossAppearance(active: Boolean) {
        if (crossActive == active) return
        crossActive = active
        crossImg?.alpha = if (active) 1f else crossBaseAlpha
        crossView?.background = crossTargetDrawable(active)
        dangerTint?.visibility = if (active) View.VISIBLE else View.GONE
    }

    private fun crossTargetDrawable(active: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(if (active) 0xFFFF3B30.toInt() else 0xFFD92D25.toInt())
        setStroke(dp(2), if (active) 0xFFFF453A.toInt() else 0xDFFF453A.toInt())
    }

    private fun clampPosition() {
        if (docked || expanded) return
        val layout = params ?: return
        val bounds = if (Build.VERSION.SDK_INT >= 30) wm?.currentWindowMetrics?.bounds else null
        val metrics = resources.displayMetrics
        layout.x = layout.x.coerceIn(0, ((bounds?.width() ?: metrics.widthPixels) - layout.width).coerceAtLeast(0))
        // LayoutParams already applies the requested top inset. Subtracting system bars
        // here a second time created the visible bottom "wall" on Samsung devices.
        val height = bounds?.height() ?: metrics.heightPixels
        layout.y = layout.y.coerceIn(0, (height - layout.height).coerceAtLeast(0))
    }

    // Read both overlay locations from Android. Reconstructing either rectangle from
    // displayMetrics drifts on gesture navigation, cutouts and OEM window insets.
    private fun miniOverlapsCross(): Boolean {
        val mini = root ?: return false
        val cross = crossView ?: return false
        if (!mini.isAttachedToWindow || !cross.isAttachedToWindow) return false
        val miniLocation = IntArray(2)
        val crossLocation = IntArray(2)
        mini.getLocationOnScreen(miniLocation)
        cross.getLocationOnScreen(crossLocation)
        val miniBounds = Rect(
            miniLocation[0], miniLocation[1],
            miniLocation[0] + mini.width, miniLocation[1] + mini.height,
        )
        // Match the visible circular target, not its square WindowManager bounds.
        // Rect.intersects previously accepted invisible corner pixels outside the ring.
        val centerX = crossLocation[0] + cross.width / 2f
        val centerY = crossLocation[1] + cross.height / 2f
        val nearestX = centerX.coerceIn(miniBounds.left.toFloat(), miniBounds.right.toFloat())
        val nearestY = centerY.coerceIn(miniBounds.top.toFloat(), miniBounds.bottom.toFloat())
        val dx = nearestX - centerX
        val dy = nearestY - centerY
        val radius = minOf(cross.width, cross.height) / 2f
        return dx * dx + dy * dy <= radius * radius
    }

    private fun updateRootLayout() {
        val view = root ?: return
        val layout = params ?: return
        runCatching { wm?.updateViewLayout(view, layout) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        crossParams?.let { layout ->
            layout.y = dp(crossMargin) + crossRaisePx
            crossView?.let { view -> runCatching { wm?.updateViewLayout(view, layout) } }
        }
        applyModeLayout()
        updateRootLayout()
        if (!docked) savePosition()
    }

    private fun savePosition() {
        if (docked || expanded) return
        val layout = params ?: return
        getSharedPreferences(POSITION_PREFS, MODE_PRIVATE).edit {
            putInt(POSITION_X, layout.x)
            putInt(POSITION_Y, layout.y)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun detachedSizePx(): Pair<Int, Int> {
        val size = controller?.videoSize
        val aspect = if (isVideo.value) {
            if (size != null && size.height > 0) size.width.toFloat() * size.pixelWidthHeightRatio / size.height
            else 16f / 9f
        } else artworkAspect
        return MiniWindowMetrics.detachedSizePx(resources.displayMetrics.density, aspect)
    }
    private fun miniWidthPx() = if (docked || expanded) WindowManager.LayoutParams.MATCH_PARENT
        else detachedSizePx().first
    private fun miniHeightPx() = if (expanded) WindowManager.LayoutParams.MATCH_PARENT
        else if (docked) MiniWindowMetrics.heightPx(resources.displayMetrics.density)
        else detachedSizePx().second

    private fun switchMode(toDocked: Boolean) {
        switchMode(if (toDocked) PlayerWindowMode.DOCKED else PlayerWindowMode.DETACHED)
    }

    private fun switchMode(target: PlayerWindowMode) {
        if (mode == target) return
        if (target == PlayerWindowMode.EXPANDED && !ensureExpandedHost()) return
        if (mode == PlayerWindowMode.DETACHED) savePosition()
        val leavingExpanded = expanded
        if (leavingExpanded && target == PlayerWindowMode.DETACHED) detachUntilLibraryHidden = true
        mode = target
        modeSnapshot.value = target
        com.local.listentomusic.ui.components.VideoSurfaceOwner.setUnifiedExpanded(expanded)
        dragging = false
        crossView?.visibility = View.INVISIBLE
        if (leavingExpanded) {
            modeState.value = mode
            expandedView?.visibility = View.GONE
            compact?.setExpanded(false)
            expandedFullscreen = false
        }
        if (expanded) {
            compact?.setExpanded(true)
            compact?.visibility = View.GONE
            expandedView?.visibility = View.VISIBLE
            modeState.value = mode
        } else {
            compact?.setDetached(!docked)
            compact?.visibility = View.VISIBLE
        }
        applyModeLayout()
        updateRootLayout()
        updateVisibility()
    }

    private fun applyModeLayout() {
        val layout = params ?: return
        layout.width = miniWidthPx()
        layout.height = miniHeightPx()
        layout.gravity = if (docked) Gravity.BOTTOM or Gravity.LEFT else Gravity.TOP or Gravity.LEFT
        layout.flags = if (expanded) {
            (layout.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv() and
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()) or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        } else {
            (layout.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) and
                WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv() and
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv()
        }
        layout.dimAmount = if (expanded) .14f else 0f
        if (expanded) {
            layout.x = 0
            layout.y = 0
            if (Build.VERSION.SDK_INT >= 30) {
                layout.setFitInsetsTypes(if (expandedFullscreen) 0 else WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                // Dock and Mini fit only one inset side. Restore all sides for
                // expanded mode or its content can be clipped on tall phones.
                layout.setFitInsetsSides(if (expandedFullscreen) 0 else WindowInsets.Side.all())
            }
        } else if (docked) {
            layout.x = 0
            layout.y = 0
            if (Build.VERSION.SDK_INT >= 30) {
                layout.setFitInsetsTypes(WindowInsets.Type.navigationBars())
                layout.setFitInsetsSides(WindowInsets.Side.BOTTOM)
            }
        } else {
            val saved = getSharedPreferences(POSITION_PREFS, MODE_PRIVATE)
            layout.x = saved.getInt(POSITION_X, dp(12))
            layout.y = saved.getInt(POSITION_Y, dp(300))
            if (Build.VERSION.SDK_INT >= 30) {
                layout.setFitInsetsTypes(WindowInsets.Type.statusBars())
                layout.setFitInsetsSides(WindowInsets.Side.TOP)
            }
            clampPosition()
        }
    }

    private fun updateVisibility() {
        val visible = ready && !fullscreenActivityActive && if (expanded) true else if (docked) PlayerWindowVisibility.dockedVisible.value else
            PlayerWindowVisibility.detachedVisible.value
        params?.let {
            it.alpha = if (visible) 1f else 0f
            it.flags = if (visible) it.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                else it.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        updateRootLayout()
    }

    private fun updateMiniWindowSize() {
        val layout = params ?: return
        val width = miniWidthPx()
        val height = miniHeightPx()
        if (layout.width == width && layout.height == height) return
        layout.width = width
        layout.height = height
        clampPosition()
        updateRootLayout()
    }

    private fun collapseExpanded() {
        if (!expanded) return
        detachUntilLibraryHidden = true
        switchMode(PlayerWindowMode.DETACHED)
        // Close the Activity underneath too; otherwise its visible Library immediately
        // requests a docked player and creates a needless DETACHED→DOCKED→DETACHED hop.
        sendBroadcast(Intent(MainActivity.ACTION_BACKGROUND_PLAYER).setPackage(packageName))
    }

    private fun returnToLibrary() {
        if (!expanded) return
        runCatching {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION
            })
        }
        switchMode(PlayerWindowMode.DOCKED)
    }

    private fun updateExpandedFullscreen(value: Boolean) {
        if (!expanded || expandedFullscreen == value) return
        expandedFullscreen = value
        params?.let { layout ->
            layout.flags = if (value) layout.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                else layout.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv() and
                    WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
            if (Build.VERSION.SDK_INT >= 30) {
                layout.setFitInsetsTypes(if (value) 0 else WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                layout.setFitInsetsSides(if (value) 0 else WindowInsets.Side.all())
                runCatching {
                    if (value) expandedView?.windowInsetsController?.hide(WindowInsets.Type.systemBars())
                    else expandedView?.windowInsetsController?.show(WindowInsets.Type.systemBars())
                }
            }
            updateRootLayout()
        }
    }

    private fun openLandscapeFullscreen() {
        if (!expanded || fullscreenActivityActive) return
        com.local.listentomusic.ui.components.VideoSurfaceOwner.beginHandoff("NOW_PLAYING")
        fullscreenActivityActive = true
        runCatching {
            startActivity(Intent(this, FullscreenVideoActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            })
            switchMode(PlayerWindowMode.DETACHED)
            updateVisibility()
        }.onFailure {
            fullscreenActivityActive = false
            com.local.listentomusic.ui.components.VideoSurfaceOwner.finishHandoff("NOW_PLAYING")
            switchMode(PlayerWindowMode.EXPANDED)
            updateVisibility()
        }
    }

    private fun dragExpanded(amount: Float) {
        if (!expanded || sharing) return
        windowAnimator?.cancel()
        params?.let {
            it.y = (it.y + amount.toInt()).coerceIn(0, it.height.coerceAtLeast(1))
            it.flags = it.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            updateRootLayout()
        }
    }

    private fun finishExpandedPull() {
        val position = params?.y ?: return
        if (position >= dp(72)) returnToLibrary() else resetExpandedPull()
    }

    private fun resetExpandedPull() {
        val layout = params ?: return
        windowAnimator?.cancel()
        windowAnimator = ValueAnimator.ofInt(layout.y, 0).apply {
            duration = 180L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                layout.y = it.animatedValue as Int
                if (layout.y == 0 && !expandedFullscreen)
                    layout.flags = layout.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv()
                updateRootLayout()
            }
            start()
        }
    }

    private fun launchShare(chooser: Intent) {
        if (!expanded || sharing) return
        val layout = params ?: return
        sharing = true
        shareInProgress = true
        savedWindowFlags = layout.flags
        layout.alpha = 0f
        layout.dimAmount = 0f
        layout.flags = layout.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        updateRootLayout()
        runCatching {
            startActivity(Intent(this, ShareProxyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(ShareProxyActivity.EXTRA_CHOOSER, chooser)
            })
        }.onFailure { restoreAfterShare() }
    }

    private fun restoreAfterShare() {
        if (!sharing) return
        sharing = false
        shareInProgress = false
        params?.let {
            it.flags = savedWindowFlags
            it.dimAmount = if (expanded) .14f else 0f
            updateVisibility()
        }
    }

    private var lastArtworkData: ByteArray? = null
    private fun updateArtwork(data: ByteArray?) {
        if (data === lastArtworkData || (data != null && data.contentEquals(lastArtworkData))) return
        lastArtworkData = data
        val generation = ++artworkGeneration
        if (data == null) {
            artworkAspect = 1f
            compact?.setArtwork(null)
            updateMiniWindowSize()
            return
        }
        scope.launch {
            val decoded = withContext(Dispatchers.Default) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 256 || bounds.outHeight / sample > 256) sample *= 2
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, BitmapFactory.Options().apply { inSampleSize = sample })
                bitmap
            }
            if (generation == artworkGeneration) {
                artworkAspect = decoded?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: 1f
                compact?.setArtwork(decoded)
                updateMiniWindowSize()
            }
        }
    }

    override fun onDestroy() {
        destinationReady.value = false
        shareInProgress = false
        modeSnapshot.value = null
        identities.value = "controllerId=none playerViewId=none"
        windowAnimator?.cancel()
        if (shareReceiverRegistered) runCatching { unregisterReceiver(shareFinished) }
        if (homeReceiverRegistered) runCatching { unregisterReceiver(systemHome) }
        if (expandedHostReady) lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        scope.cancel()
        com.local.listentomusic.ui.components.VideoSurfaceOwner.setUnifiedExpanded(false)
        com.local.listentomusic.ui.components.VideoSurfaceOwner.serviceEvent("stop")
        com.local.listentomusic.ui.components.VideoSurfaceOwner.setSystemOverlayVisible(
            "mini_window_overlay",
            "MINI_WINDOW",
            false,
        )
        com.local.listentomusic.ui.components.VideoSurfaceOwner.finishHandoff("MINI_WINDOW")
        root?.removeCallbacks(dragFrame)
        root?.let { runCatching { wm?.removeViewImmediate(it) } }
        crossView?.let { runCatching { wm?.removeViewImmediate(it) } }
        videoView?.let(com.local.listentomusic.ui.components.VideoSurfaceOwner::detach)
        controller?.removeListener(listener)
        controller = null
        compact?.release()
        compact = null
        connectionLease?.close()
        connectionLease = null
        future = null
        root = null
        expandedView = null
        crossView = null
        crossParams = null
        wm = null
        viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
