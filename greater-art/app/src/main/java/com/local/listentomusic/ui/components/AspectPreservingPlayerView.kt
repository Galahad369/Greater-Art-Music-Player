package com.local.listentomusic.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/** Keep Media3's player/surface, but never size its video as a stretched viewport. */
@androidx.annotation.OptIn(UnstableApi::class)
class AspectPreservingPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PlayerView(context, attrs) {
    private var sourceAspectRatio = 0f

    init {
        clipChildren = true
        clipToPadding = true
        // PlayerView reads XML resize_mode without invoking this override.
        setResizeMode(resizeMode)
    }

    override fun setPlayer(player: Player?) {
        super.setPlayer(player)
        // MediaController commands/tracks can arrive after attachment. Media3's
        // initial aspect refresh is conditional; a valid cached VideoSize is not.
        synchronizeVideoGeometry()
    }

    internal fun synchronizeVideoGeometry() {
        player?.videoSize?.let { size ->
            applyVideoAspectRatio(sourceVideoAspectRatio(size.width, size.height, size.pixelWidthHeightRatio) ?: 0f)
        }
    }

    override fun setResizeMode(resizeMode: Int) {
        super.setResizeMode(if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT)
        updateContentSize(width, height)
    }

    override fun onContentAspectRatioChanged(contentFrame: AspectRatioFrameLayout?, aspectRatio: Float) {
        // Own the ratio and its measured rectangle together. Do not briefly clear
        // a retained frame to Media3's unknown-aspect (stretch-to-viewport) state.
        applyVideoAspectRatio(aspectRatio)
    }

    internal fun applyVideoAspectRatio(aspectRatio: Float) {
        if (aspectRatio.isFinite() && aspectRatio > 0f) sourceAspectRatio = aspectRatio
        if (sourceAspectRatio <= 0f) return
        // A temporary unknown size must not deform the last retained video frame.
        updateContentSize(width, height)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (View.MeasureSpec.getMode(widthMeasureSpec) == View.MeasureSpec.EXACTLY &&
            View.MeasureSpec.getMode(heightMeasureSpec) == View.MeasureSpec.EXACTLY) {
            // Constrain before measuring children, including the first frame after
            // rotation. This also removes Media3's intentional 1% deformation band.
            updateContentSize(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec))
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (updateContentSize(measuredWidth, measuredHeight)) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateContentSize(w, h)
    }

    private fun updateContentSize(viewWidth: Int, viewHeight: Int): Boolean {
        if (sourceAspectRatio <= 0f) return false
        val frame = findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame) ?: return false
        val layout = frame.layoutParams as? FrameLayout.LayoutParams ?: return false
        val size = aspectPreservingVideoSize(
            viewWidth - paddingLeft - paddingRight - layout.leftMargin - layout.rightMargin,
            viewHeight - paddingTop - paddingBottom - layout.topMargin - layout.bottomMargin,
            sourceAspectRatio,
            resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        ) ?: return false
        frame.setAspectRatio(sourceAspectRatio)
        if (layout.width == size.width && layout.height == size.height && layout.gravity == Gravity.CENTER) return false
        layout.width = size.width
        layout.height = size.height
        layout.gravity = Gravity.CENTER
        frame.layoutParams = layout
        return true
    }
}
