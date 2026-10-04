package com.local.listentomusic.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import com.local.listentomusic.R
import com.local.listentomusic.model.MiniWindowMetrics
import androidx.compose.ui.graphics.toArgb
import com.local.listentomusic.data.AppFont
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.ui.uiText
import com.local.listentomusic.ui.theme.appColorScheme

internal fun shouldRefreshCompactBinding(samePlayer: Boolean, samePresentation: Boolean): Boolean =
    !samePlayer || !samePresentation

/** One compact UI, used unchanged by the Compose and WindowManager hosts. */
class CompactPlayerView(context: Context) : FrameLayout(context) {
    val preview = FrameLayout(context)
    val artwork = ImageView(context)
    val video = LayoutInflater.from(context).inflate(R.layout.background_video, preview, false) as PlayerView
    private val title = TextView(context)
    private val previous = button(android.R.drawable.ic_media_previous)
    private val toggle = button(android.R.drawable.ic_media_play)
    private val next = button(android.R.drawable.ic_media_next)
    private val progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
    private var player: Player? = null
    private var owner = "LIBRARY_MINI"
    private var lastArtwork: Bitmap? = null
    private var previousLabel = "Previous"
    private var playLabel = "Play"
    private var pauseLabel = "Pause"
    private var nextLabel = "Next"
    private var appearanceKey: List<Any>? = null
    private var paletteBackground = Color.rgb(22, 30, 28)
    private var detached = false
    private var expanded = false
    var onOpen: () -> Unit = {}
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { refresh() }
    }
    private val ticker = object : Runnable {
        override fun run() {
            val p = player ?: return
            progress.progress = if (p.duration > 0) ((p.currentPosition.toDouble() / p.duration) * 1000).toInt().coerceIn(0, 1000) else 0
            postDelayed(this, 250)
        }
    }
    init {
        setBackgroundColor(Color.rgb(22, 30, 28))
        val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        addView(row, LayoutParams(-1, -1))
        row.addView(preview, LinearLayout.LayoutParams(MiniWindowMetrics.squareWidthPx(resources.displayMetrics.density), -1))
        artwork.scaleType = ImageView.ScaleType.CENTER_CROP
        artwork.setImageResource(R.drawable.ic_launcher_foreground)
        preview.addView(artwork, LayoutParams(-1, -1))
        video.useController = false
        video.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        video.setKeepContentOnPlayerReset(true)
        video.isClickable = false
        preview.addView(video, LayoutParams(-1, -1))
        title.setTextColor(Color.WHITE)
        title.textSize = 14f
        title.maxLines = 1
        title.ellipsize = android.text.TextUtils.TruncateAt.END
        title.setPadding(dp(8f), 0, dp(4f), 0)
        row.addView(title, LinearLayout.LayoutParams(0, -1, 1f))
        title.gravity = Gravity.CENTER_VERTICAL
        listOf(previous, toggle, next).forEach { row.addView(it, LinearLayout.LayoutParams(dp(48f), -1)) }
        previous.visibility = View.GONE
        next.visibility = View.GONE
        progress.max = 1000
        progress.progressTintList = ColorStateList.valueOf(0xFF8FE3CF.toInt())
        addView(progress, LayoutParams(-1, dp(2f), Gravity.BOTTOM))
        setOnClickListener { onOpen() }
        previous.setOnClickListener { player?.let { if (it.currentPosition > 4000L) it.seekTo(0) else it.seekToPreviousMediaItem() } }
        toggle.setOnClickListener { player?.let { if (it.isPlaying) it.pause() else it.play() } }
        next.setOnClickListener { player?.seekToNextMediaItem() }
    }
    private fun button(icon: Int) = ImageButton(context).apply {
        setImageResource(icon)
        imageTintList = ColorStateList.valueOf(0xFF8FE3CF.toInt())
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setPadding(dp(10f), dp(10f), dp(10f), dp(10f))
        background = RippleDrawable(ColorStateList.valueOf(0x338FE3CF), null, null)
    }
    private fun dp(value: Float) = (value * resources.displayMetrics.density).toInt()
    fun labels(previousLabel: String, playLabel: String, pauseLabel: String, nextLabel: String) {
        this.previousLabel = previousLabel
        this.playLabel = playLabel
        this.pauseLabel = pauseLabel
        this.nextLabel = nextLabel
        previous.contentDescription = previousLabel
        toggle.contentDescription = if (player?.isPlaying == true) pauseLabel else playLabel
        next.contentDescription = nextLabel
    }
    fun colors(background: Int, foreground: Int, accent: Int) {
        paletteBackground = background
        setBackgroundColor(if (detached) Color.TRANSPARENT else background)
        title.setTextColor(foreground)
        listOf(previous, toggle, next).forEach { it.imageTintList = ColorStateList.valueOf(accent) }
        progress.progressTintList = ColorStateList.valueOf(accent)
    }
    fun appearance(settings: UserPreferences) {
        val night = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val font = if (settings.silianRail) AppFont.SILIAN_RAIL else settings.appFont
        val key = listOf(settings.themeMode, settings.colorTheme, font, settings.appLanguage, night)
        if (appearanceKey == key) return
        appearanceKey = key
        val dark = settings.themeMode == ThemeMode.DARK || (settings.themeMode == ThemeMode.SYSTEM && night)
        val palette = appColorScheme(settings.colorTheme, dark)
        colors(palette.surface.toArgb(), palette.onSurface.toArgb(), palette.secondary.toArgb())
        val lang = settings.appLanguage
        labels(uiText(lang, "Previous", "上一首"), uiText(lang, "Play", "播放"), uiText(lang, "Pause", "暫停"), uiText(lang, "Next", "下一首"))
        val resource = when (font) {
            AppFont.INTER -> R.font.inter
            AppFont.NUNITO -> R.font.nunito
            AppFont.OSWALD -> R.font.oswald
            AppFont.SILIAN_RAIL -> R.font.garamond
            AppFont.PLAYFAIR_DISPLAY -> R.font.playfair_display
            AppFont.ROBOTO_SLAB -> R.font.roboto_slab
            AppFont.SOURCE_CODE_PRO -> R.font.source_code_pro
            else -> 0
        }
        title.typeface = if (resource != 0) androidx.core.content.res.ResourcesCompat.getFont(context, resource)
        else android.graphics.Typeface.create(when (font) {
            AppFont.SERIF -> "serif"
            AppFont.MONOSPACE -> "monospace"
            AppFont.CURSIVE -> "cursive"
            else -> "sans-serif"
        }, android.graphics.Typeface.NORMAL)
        title.isAllCaps = font == AppFont.SILIAN_RAIL
        title.fontFeatureSettings = if (font == AppFont.SILIAN_RAIL) "smcp" else null
    }
    fun setArtwork(bitmap: Bitmap?) {
        if (lastArtwork === bitmap) return
        lastArtwork = bitmap
        if (bitmap == null) artwork.setImageResource(R.drawable.ic_launcher_foreground) else artwork.setImageBitmap(bitmap)
    }
    private fun updateDetachedLayout() {
        // Keep the whole detached window transparent even after theme updates.
        if (detached) {
            setBackgroundColor(Color.TRANSPARENT)
            preview.setBackgroundColor(Color.TRANSPARENT)
            val lp = preview.layoutParams as LinearLayout.LayoutParams
            lp.width = LayoutParams.MATCH_PARENT
            lp.height = LayoutParams.MATCH_PARENT
            lp.setMargins(0, 0, 0, 0)
            preview.layoutParams = lp
        } else {
            setBackgroundColor(paletteBackground)
            val lp = preview.layoutParams as LinearLayout.LayoutParams
            lp.width = MiniWindowMetrics.squareWidthPx(resources.displayMetrics.density)
            lp.height = LayoutParams.MATCH_PARENT
            preview.layoutParams = lp
        }
    }

    fun setDetached(value: Boolean) {
        if (detached == value) return
        detached = value
        listOf(title, toggle, progress).forEach {
            it.visibility = if (value) View.GONE else View.VISIBLE
        }
        removeCallbacks(ticker)
        if (!value && player != null) post(ticker)
        updateDetachedLayout()
        refresh()
    }
    fun setExpanded(value: Boolean) {
        expanded = value
        if (!value) restoreVideo()
        refresh()
    }
    fun restoreVideo() {
        if (video.parent === preview) return
        (video.parent as? ViewGroup)?.removeView(video)
        preview.addView(video, LayoutParams(-1, -1))
    }
    fun bind(value: Player?, presentation: String) {
        val samePlayer = player === value
        val samePresentation = owner == presentation
        if (!shouldRefreshCompactBinding(samePlayer, samePresentation)) return
        owner = presentation
        video.tag = presentation
        if (!samePlayer) {
            player?.removeListener(listener)
            player = value
            value?.addListener(listener)
            removeCallbacks(ticker)
            if (value != null && !detached) post(ticker)
        }
        refresh()
    }
    private fun refresh() {
        val p = player ?: return
        title.text = p.mediaMetadata.title ?: p.currentMediaItem?.mediaId?.substringAfterLast('/')
        previous.isEnabled = p.hasPreviousMediaItem()
        next.isEnabled = p.hasNextMediaItem()
        toggle.setImageResource(if (p.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
        toggle.contentDescription = if (p.isPlaying) pauseLabel else playLabel
        val isVideo = p.currentMediaItem?.mediaMetadata?.mediaType == androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO ||
            p.currentMediaItem?.mediaId?.substringAfterLast('.')?.lowercase() in setOf("mp4", "mkv", "webm", "mov", "m4v", "avi", "3gp", "ts", "mpeg", "mpg", "flv")
        video.visibility = if (isVideo) View.VISIBLE else View.GONE
        artwork.visibility = if (isVideo) View.GONE else View.VISIBLE
        val ratio = if (isVideo && p.videoSize.height > 0) p.videoSize.width.toFloat() * p.videoSize.pixelWidthHeightRatio / p.videoSize.height
            else lastArtwork?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: 1f
        val width = if (detached) LayoutParams.MATCH_PARENT else if (MiniWindowMetrics.isSquareAspect(ratio))
            MiniWindowMetrics.squareWidthPx(resources.displayMetrics.density)
        else MiniWindowMetrics.widthPx(resources.displayMetrics.density)
        if (preview.layoutParams.width != width) { preview.layoutParams = preview.layoutParams.apply { this.width = width } }
        if (!expanded) {
            if (isVideo) VideoSurfaceOwner.attach(p, video, overlay = owner == "MINI_WINDOW")
            else VideoSurfaceOwner.detach(video)
            // One decoded stream has one surface. When CURRENT_VIDEO owns it,
            // retain artwork in Dock instead of presenting an empty black view.
            video.visibility = if (isVideo && video.player === p) View.VISIBLE else View.GONE
            artwork.visibility = if (isVideo && video.player === p) View.GONE else View.VISIBLE
        }
    }
    fun release() {
        removeCallbacks(ticker)
        player?.removeListener(listener)
        VideoSurfaceOwner.detach(video)
        player = null
    }

    internal fun inspectionBounds(): List<Pair<String, android.graphics.Rect>> = listOf(
        "MINI_PREVIEW" to preview, "MINI_TITLE" to title,
        "MINI_PREVIOUS_BUTTON" to previous, "MINI_PLAY_PAUSE_BUTTON" to toggle,
        "MINI_NEXT_BUTTON" to next, "MINI_PROGRESS" to progress,
    ).map { (label, view) ->
        label to android.graphics.Rect(0, 0, view.width, view.height).also {
            offsetDescendantRectToMyCoords(view, it)
        }
    }
}
