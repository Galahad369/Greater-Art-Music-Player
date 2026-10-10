package com.local.listentomusic.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.local.listentomusic.ui.theme.GaInspectorPalette

internal data class InspectorRegion(
    val label: String,
    val detail: String,
    val bounds: Rect,
    val order: Long,
)

@Stable
internal class UiInspectorState {
    private class Entry(val label: String, val detail: String, val order: Long)

    // Snapshot state holds only membership + labels. Coordinates live in a plain map so
    // scrolling (which repositions every row each frame) never writes snapshot state and
    // never recomposes the diagnostics readers at the app root.
    private val entries = mutableStateMapOf<Any, Entry>()
    private val layouts = HashMap<Any, LayoutCoordinates>()
    // Unit tests and non-layout callers can inject fixed bounds without putting
    // per-frame production geometry back into snapshot state.
    private val fixedBounds = HashMap<Any, Rect>()
    internal val regions: Map<Any, InspectorRegion>
        get() = entries.mapNotNull { (key, entry) ->
            boundsOf(key)?.let { key to InspectorRegion(entry.label, entry.detail, it, entry.order) }
        }.toMap()
    private val hitProviders = mutableMapOf<Any, (Offset) -> InspectorRegion?>()
    var armed by mutableStateOf(false)
    var selected by mutableStateOf<InspectorRegion?>(null)
        private set
    var matchingRegions by mutableStateOf<List<InspectorRegion>>(emptyList())
        private set
    var selectedMatchIndex by mutableIntStateOf(0)
        private set
    var lastTouch by mutableStateOf(Offset.Zero)
        private set
    private var order = 0L

    fun update(key: Any, label: String, detail: String, coordinates: LayoutCoordinates) {
        layouts[key] = coordinates
        fixedBounds.remove(key)
        updateEntry(key, label, detail)
    }

    internal fun update(key: Any, label: String, detail: String, bounds: Rect) {
        layouts.remove(key)
        fixedBounds[key] = bounds
        updateEntry(key, label, detail)
    }

    private fun updateEntry(key: Any, label: String, detail: String) {
        val previous = entries[key]
        if (previous?.label == label && previous.detail == detail) return
        entries[key] = Entry(label, detail, previous?.order ?: ++order)
    }

    private fun boundsOf(key: Any): Rect? {
        val bounds = layouts[key]?.takeIf { it.isAttached }?.boundsInRoot() ?: fixedBounds[key] ?: return null
        return bounds.takeIf {
            it.left.isFinite() && it.top.isFinite() && it.width > 0f && it.height > 0f
        }
    }

    fun remove(key: Any) { entries.remove(key); layouts.remove(key); fixedBounds.remove(key) }
    fun setHitProvider(key: Any, provider: (Offset) -> InspectorRegion?) { hitProviders[key] = provider }
    fun removeHitProvider(key: Any) { hitProviders.remove(key) }
    fun arm() { selected = null; matchingRegions = emptyList(); selectedMatchIndex = 0; armed = true }
    fun cancel() { armed = false }
    fun clearSelection() { selected = null; matchingRegions = emptyList(); selectedMatchIndex = 0 }
    fun clear() { armed = false; clearSelection(); entries.clear(); layouts.clear(); fixedBounds.clear(); hitProviders.clear() }

    fun pick(point: Offset) {
        lastTouch = point
        val preciseHits = hitProviders.values.mapNotNull { it(point) }
        matchingRegions = (preciseHits + regions.values).asSequence()
            .filter { it.bounds.contains(point) }
            .sortedWith(compareBy<InspectorRegion> { it.bounds.width * it.bounds.height }.thenByDescending { it.order })
            .toList()
        selectedMatchIndex = 0
        selected = matchingRegions.firstOrNull()
            ?: InspectorRegion("WINDOW_BACKGROUND", "No interactive element at this point", Rect(point, 1f), Long.MAX_VALUE)
        armed = false
    }

    fun selectNextMatch() {
        if (matchingRegions.size < 2) return
        selectedMatchIndex = (selectedMatchIndex + 1) % matchingRegions.size
        selected = matchingRegions[selectedMatchIndex]
    }
}

internal val LocalUiInspector = compositionLocalOf<UiInspectorState?> { null }

internal fun Modifier.inspectElement(label: String, detail: String = ""): Modifier = composed {
    val inspector = LocalUiInspector.current ?: return@composed this
    val key = remember { Any() }
    DisposableEffect(inspector, key) { onDispose { inspector.remove(key) } }
    onGloballyPositioned { inspector.update(key, label, detail, it) }
}

@Composable
internal fun UiInspectorHost(
    enabled: Boolean,
    state: UiInspectorState,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    // Inspector colors are independent of the selected app theme. Debug text must
    // remain readable even when a custom background or broken palette is under it.
    val inspectorError = GaInspectorPalette.error
    val inspectorAccent = GaInspectorPalette.accent
    LaunchedEffect(enabled) { if (!enabled) state.clear() }
    BackHandler(enabled && state.armed) { state.cancel() }
    CompositionLocalProvider(LocalUiInspector provides if (enabled) state else null) {
        Box(Modifier.fillMaxSize()) {
            content()
            if (enabled && state.armed) {
                Box(
                    Modifier.matchParentSize().zIndex(100f)
                        .background(Color.Black.copy(alpha = 0.08f))
                        .pointerInput(Unit) { detectTapGestures { state.pick(it) } },
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp).zIndex(101f),
                    color = Color.Black.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("TAP AN ELEMENT", Modifier.padding(horizontal = 12.dp), color = Color.White,
                            fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = state::cancel) { Icon(Icons.Rounded.Close, "Cancel inspector", tint = Color.White) }
                    }
                }
            }
            if (enabled) state.selected?.let { region ->
                Canvas(Modifier.matchParentSize().zIndex(98f)) {
                    drawRect(inspectorError.copy(alpha = 0.10f), region.bounds.topLeft, region.bounds.size)
                    drawRect(inspectorError, region.bounds.topLeft, region.bounds.size,
                        style = Stroke(2.dp.toPx()))
                    drawLine(inspectorAccent, Offset(state.lastTouch.x - 10.dp.toPx(), state.lastTouch.y),
                        Offset(state.lastTouch.x + 10.dp.toPx(), state.lastTouch.y), 1.dp.toPx())
                    drawLine(inspectorAccent, Offset(state.lastTouch.x, state.lastTouch.y - 10.dp.toPx()),
                        Offset(state.lastTouch.x, state.lastTouch.y + 10.dp.toPx()), 1.dp.toPx())
                }
                val report = remember(region, state.lastTouch, density.density) {
                    buildString {
                        appendLine(region.label)
                        if (region.detail.isNotBlank()) appendLine(region.detail)
                        appendLine("boundsPx=${region.bounds.left.toInt()},${region.bounds.top.toInt()} → ${region.bounds.right.toInt()},${region.bounds.bottom.toInt()}")
                        appendLine("sizePx=${region.bounds.width.toInt()}×${region.bounds.height.toInt()}")
                        appendLine("touchPx=${state.lastTouch.x.toInt()},${state.lastTouch.y.toInt()}")
                        if (state.matchingRegions.isNotEmpty()) appendLine("stack=${state.selectedMatchIndex + 1}/${state.matchingRegions.size}")
                        append("sizeDp=${(region.bounds.width / density.density).toInt()}×${(region.bounds.height / density.density).toInt()}")
                    }
                }
                Surface(
                    // Library's detached-size dock is a separate WindowManager
                    // surface. Keep the inspector's actions above that dock.
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, bottom = 76.dp).zIndex(99f),
                    color = GaInspectorPalette.surface.copy(alpha = 0.98f),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 6.dp,
                ) {
                    Column(Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, end = 6.dp, bottom = 6.dp)) {
                        Text(report, Modifier.fillMaxWidth(), color = Color.White,
                            fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically) {
                            if (state.matchingRegions.size > 1) {
                                TextButton(onClick = state::selectNextMatch) {
                                    Text("NEXT ${state.selectedMatchIndex + 1}/${state.matchingRegions.size}", color = inspectorAccent)
                                }
                            }
                            TextButton(onClick = {
                                context.getSystemService(ClipboardManager::class.java)
                                    ?.setPrimaryClip(ClipData.newPlainText("Greater Art element", report))
                            }) { Text("COPY", color = inspectorAccent) }
                            IconButton(onClick = state::clearSelection) { Icon(Icons.Rounded.Close, "Close selection", tint = Color.White) }
                        }
                    }
                }
            }
        }
    }
}
