package com.local.listentomusic.ui.components

import android.view.ContextThemeWrapper
import android.view.View
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.R
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Native frame/surface geometry, not proof of decoder pixels or OEM compositor output. */
@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(UnstableApi::class)
class AspectPreservingPlayerViewGeometryTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun fitColdStartKeepsWideVideoInsidePortraitViewport() = onMain {
        val view = playerView()
        view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        view.applyVideoAspectRatio(16f / 9f)
        layout(view, 1080, 2340)
        assertGeometry(view, 1080, 2340, 16f / 9f, crop = false)
        assertTrue("Fit must letterbox this portrait viewport", contentFrame(view).height < view.height)
    }

    @Test fun cropColdStartCoversPortraitViewportWithoutChangingWideAspect() = onMain {
        val view = playerView()
        view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        view.applyVideoAspectRatio(16f / 9f)
        layout(view, 1080, 2340)
        assertGeometry(view, 1080, 2340, 16f / 9f, crop = true)
        assertTrue("Crop must extend the wide frame beyond the viewport", contentFrame(view).width > view.width)
    }

    @Test fun rotationAndModeChangesRetainOneNativeVideoSurface() = onMain {
        val view = playerView()
        val surface = requireNotNull(view.videoSurfaceView)
        view.applyVideoAspectRatio(16f / 9f)
        for (crop in listOf(false, true, false)) {
            view.resizeMode = if (crop) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                else AspectRatioFrameLayout.RESIZE_MODE_FIT
            for ((width, height) in listOf(1080 to 2340, 2340 to 1080, 1080 to 2340)) {
                layout(view, width, height)
                assertGeometry(view, width, height, 16f / 9f, crop)
                assertSame("A viewport change must not create another video surface", surface, view.videoSurfaceView)
            }
        }
    }

    @Test fun portraitSquareAndAnamorphicSourcesKeepTheirDisplayedAspect() = onMain {
        val view = playerView()
        val ratios = listOf(9f / 16f, 1f,
            requireNotNull(sourceVideoAspectRatio(1440, 1080, 4f / 3f)))
        for (ratio in ratios) {
            view.applyVideoAspectRatio(ratio)
            for (crop in listOf(false, true)) {
                view.resizeMode = if (crop) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else AspectRatioFrameLayout.RESIZE_MODE_FIT
                for ((width, height) in listOf(1080 to 2340, 2340 to 1080, 721 to 1283)) {
                    layout(view, width, height)
                    assertGeometry(view, width, height, ratio, crop)
                }
            }
        }
    }

    @Test fun closeAspectDoesNotUseMedia3sOnePercentStretchTolerance() = onMain {
        val view = playerView()
        val ratio = 1.009f
        view.applyVideoAspectRatio(ratio)
        for (crop in listOf(false, true)) {
            view.resizeMode = if (crop) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                else AspectRatioFrameLayout.RESIZE_MODE_FIT
            layout(view, 1000, 1000)
            assertGeometry(view, 1000, 1000, ratio, crop)
            val frame = contentFrame(view)
            assertFalse("Fit/Crop must not leave a near-square source stretched into a square",
                frame.width == 1000 && frame.height == 1000)
        }
    }

    @Test fun repeatedSameRatioRepairsZeroAndStaleContentFrameAspect() {
        val ratio = 16f / 9f
        val view = onMain {
            playerView().apply {
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                applyVideoAspectRatio(ratio)
                layout(this, 1080, 2340)
            }
        }
        for (corruptRatio in listOf(0f, 1f, 9f / 16f)) {
            val observed = AtomicReference<Float?>(null)
            onMain {
                val frame = contentFrame(view)
                frame.setAspectRatioListener { target, _, _ -> observed.set(target) }
                frame.setAspectRatio(corruptRatio)
                // Reapplying the same known ratio must still repair native state.
                // Merely remembering the last input or checking resizeMode is not enough.
                view.applyVideoAspectRatio(ratio)
                layout(view, 1080, 2340)
                assertGeometry(view, 1080, 2340, ratio, crop = false)
            }
            instrumentation.waitForIdleSync()
            assertNotNull("Native aspect restoration must be observed after measurement", observed.get())
            assertEquals("The content frame must receive the current media aspect",
                ratio, requireNotNull(observed.get()), .00001f)
        }
    }

    @Test fun invalidAspectUpdatesKeepTheLastKnownGeometry() = onMain {
        val view = playerView()
        view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        view.applyVideoAspectRatio(9f / 16f)
        for (invalid in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            view.applyVideoAspectRatio(invalid)
            layout(view, 2340, 1080)
            assertGeometry(view, 2340, 1080, 9f / 16f, crop = false)
        }
    }

    @Test fun unsupportedResizeModesCannotEnableStretching() = onMain {
        val view = playerView()
        view.applyVideoAspectRatio(16f / 9f)
        for (unsupported in listOf(AspectRatioFrameLayout.RESIZE_MODE_FILL,
                AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
                AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT, -1, 99)) {
            view.resizeMode = unsupported
            assertEquals("Only Fit or Crop may reach the native content frame",
                AspectRatioFrameLayout.RESIZE_MODE_FIT, view.resizeMode)
            layout(view, 1080, 2340)
            assertGeometry(view, 1080, 2340, 16f / 9f, crop = false)
        }
    }

    private fun playerView() = AspectPreservingPlayerView(
        ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_GreaterArt),
    ).apply { useController = false }

    private fun contentFrame(view: AspectPreservingPlayerView): AspectRatioFrameLayout =
        requireNotNull(view.findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame))

    private fun layout(view: AspectPreservingPlayerView, width: Int, height: Int) {
        view.forceLayout()
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
    }

    private fun assertGeometry(view: AspectPreservingPlayerView, viewportWidth: Int,
        viewportHeight: Int, ratio: Float, crop: Boolean) {
        val expected = requireNotNull(aspectPreservingVideoSize(viewportWidth, viewportHeight, ratio, crop))
        val frame = contentFrame(view)
        val surface = requireNotNull(view.videoSurfaceView)
        assertEquals("The viewport width must remain unchanged", viewportWidth, view.width)
        assertEquals("The viewport height must remain unchanged", viewportHeight, view.height)
        assertEquals("Native frame width", expected.width, frame.width)
        assertEquals("Native frame height", expected.height, frame.height)
        assertEquals("The native video surface must follow its content frame", frame.width, surface.width)
        assertEquals("The native video surface must follow its content frame", frame.height, surface.height)
        assertTrue("The native frame must remain horizontally centred",
            abs(frame.left.toLong() + frame.right - viewportWidth) <= 1L)
        assertTrue("The native frame must remain vertically centred",
            abs(frame.top.toLong() + frame.bottom - viewportHeight) <= 1L)
        if (crop) {
            assertTrue("Crop must cover the viewport", frame.width >= viewportWidth && frame.height >= viewportHeight)
        } else {
            assertTrue("Fit must stay inside the viewport", frame.width <= viewportWidth && frame.height <= viewportHeight)
        }
        val widthError = abs(frame.width - frame.height * ratio.toDouble())
        val heightError = abs(frame.height - frame.width / ratio.toDouble())
        assertTrue("Native geometry may differ only by integer pixel rounding: ${frame.width}x${frame.height} ratio=$ratio",
            min(widthError, heightError) <= 1.0)
        assertEquals("No independent X/Y scaling may compensate for incorrect bounds",
            surface.scaleX, surface.scaleY, 0f)
    }

    private fun <T> onMain(block: () -> T): T {
        val task = FutureTask(block)
        instrumentation.runOnMainSync(task)
        return task.get(5, TimeUnit.SECONDS)
    }
}
