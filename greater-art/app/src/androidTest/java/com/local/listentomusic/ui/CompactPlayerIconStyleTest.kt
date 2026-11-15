package com.local.listentomusic.ui

import android.graphics.drawable.VectorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.test.platform.app.InstrumentationRegistry
import com.local.listentomusic.ui.components.CompactPlayerView
import com.local.listentomusic.ui.theme.GaControl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class CompactPlayerIconStyleTest {
    @Test fun nativeTransportUsesTheSameCompactVectorSizeAsCompose() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val view = CompactPlayerView(instrumentation.targetContext)
            try {
                fun buttons(child: View): List<ImageButton> = when (child) {
                    is androidx.media3.ui.PlayerView -> emptyList()
                    is ImageButton -> listOf(child)
                    is ViewGroup -> (0 until child.childCount).flatMap { buttons(child.getChildAt(it)) }
                    else -> emptyList()
                }
                val actions = buttons(view)
                assertEquals(3, actions.size)
                val iconPx = (GaControl.icon.value * view.resources.displayMetrics.density).roundToInt()
                actions.forEach { action ->
                    assertTrue("Native transport must use app-owned rounded vectors, not OEM bitmaps", action.drawable is VectorDrawable)
                    assertEquals(iconPx, action.drawable.intrinsicWidth)
                    assertEquals(iconPx, action.drawable.intrinsicHeight)
                }
            } finally { view.release() }
        }
    }
}
