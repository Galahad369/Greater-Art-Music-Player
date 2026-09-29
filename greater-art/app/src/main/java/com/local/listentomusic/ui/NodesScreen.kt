package com.local.listentomusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.local.listentomusic.graph.LibraryGraph
import com.local.listentomusic.graph.GraphOptions
import com.local.listentomusic.graph.presentGraph
import kotlin.math.*

/** Nodes is to the right of Library: swipe left to enter. The header still pages. */
@Composable
fun NodesScreen(graph: LibraryGraph?, loading: Boolean, error: String?, currentPath: String?,
    contentPadding: PaddingValues, onLibrary: () -> Unit, onRetry: () -> Unit, onPlay: (String) -> Unit,
    options: GraphOptions, onOptions: (GraphOptions) -> Unit) {
    Column(Modifier.fillMaxSize().inspectElement("NODES_SCREEN", "Filename-similarity graph and controls")
        .background(MaterialTheme.colorScheme.background.copy(alpha = .35f)).padding(contentPadding).consumeWindowInsets(contentPadding).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            .inspectElement("NODES_TOP_BAR", "Graph title and return to Library"),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onLibrary, modifier = Modifier.inspectElement("NODES_LIBRARY_BUTTON", "Returns to Library")) { Text("← Library") }
            Text("Nodes", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp))
        }
        when {
            error != null -> { Text(error, Modifier.padding(16.dp).inspectElement("NODES_ERROR", error)); TextButton(onClick = onRetry, modifier = Modifier.inspectElement("NODES_RETRY_BUTTON", "Rebuild graph")) { Text("Retry") } }
            graph == null -> { LinearProgressIndicator(Modifier.fillMaxWidth().inspectElement("NODES_LOADING", "Building filename graph")); Text("Preparing filename connections…", Modifier.padding(16.dp)) }
            graph.nodes.isEmpty() -> Text("Your library is empty. Add media in Download, then refresh Library.", Modifier.padding(16.dp).inspectElement("NODES_EMPTY", "No media files to graph"))
            else -> {
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                GraphCanvas(graph, currentPath?.let { com.local.listentomusic.model.sourceMediaPath(it) }, onPlay, Modifier.weight(1f), options, onOptions)
            }
        }
    }
}

@Composable
private fun GraphCanvas(graph: LibraryGraph, currentPath: String?, onPlay: (String) -> Unit, modifier: Modifier,
    options: GraphOptions, onOptions: (GraphOptions) -> Unit) {
    val presentation = remember(graph, options) { presentGraph(graph, options) }
    var controls by remember { mutableStateOf(false) }
    val current = remember(graph, currentPath) { graph.nodes.indexOfFirst { it.id == currentPath } }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(graph) { mutableFloatStateOf(1f) }
    var pan by remember(graph) { mutableStateOf(Offset.Zero) }
    val moved = remember(graph) { mutableStateMapOf<Int, Offset>() }
    var selected by remember(graph) { mutableIntStateOf(-1) }
    var picker by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val color = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val ink = MaterialTheme.colorScheme.onSurface
    val labelPaint = remember(ink) { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = ink.toArgb(); textSize = 28f
    } }
    val labelBacking = MaterialTheme.colorScheme.surface.copy(alpha = .88f)
    val backingPaint = remember(labelBacking) { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = labelBacking.toArgb()
    } }
    val onPlayCurrent by rememberUpdatedState(onPlay)
    val inspector = LocalUiInspector.current
    val inspectorDensity = LocalDensity.current
    val hitProviderKey = remember { Any() }
    var canvasBounds by remember { mutableStateOf(Rect.Zero) }
    DisposableEffect(inspector, hitProviderKey) {
        onDispose { inspector?.removeHitProvider(hitProviderKey) }
    }
    // Canvas nodes are not Compose children. Resolve them mathematically when the
    // inspector is tapped, instead of registering hundreds of mutable regions.
    SideEffect {
        inspector?.setHitProvider(hitProviderKey) { touch ->
            if (!canvasBounds.contains(touch)) null else {
                val center = Offset(canvasBounds.width / 2f, canvasBounds.height / 2f)
                val local = touch - canvasBounds.topLeft
                val candidates = (presentation.visibleNodes + listOfNotNull(current.takeIf { it >= 0 })).distinct()
                val nearest = candidates.minByOrNull { i ->
                    val p = (moved[i] ?: Offset(graph.points[i].x, graph.points[i].y)) * scale + pan + center
                    (p - local).getDistanceSquared()
                }
                nearest?.let { i ->
                    val p = (moved[i] ?: Offset(graph.points[i].x, graph.points[i].y)) * scale + pan + center + canvasBounds.topLeft
                    val radius = with(inspectorDensity) { 28.dp.toPx() }
                    if ((p - touch).getDistance() > radius) null else InspectorRegion(
                        "NODES_MEDIA_NODE", "${graph.nodes[i].filename} · ${if (i == current) "playing" else "tap to play"}",
                        Rect(p.x - radius, p.y - radius, p.x + radius, p.y + radius), Long.MAX_VALUE,
                    )
                }
            }
        }
    }
    fun fit() {
        val w = (graph.points.maxOf { it.x } - graph.points.minOf { it.x } + 100f).coerceAtLeast(100f)
        val h = (graph.points.maxOf { it.y } - graph.points.minOf { it.y } + 100f).coerceAtLeast(100f)
        scale = min(viewport.width / w, viewport.height / h).coerceIn(0.08f, 3f)
        pan = Offset(-(graph.points.maxOf { it.x } + graph.points.minOf { it.x }) / 2 * scale,
            -(graph.points.maxOf { it.y } + graph.points.minOf { it.y }) / 2 * scale)
        moved.clear()
    }
    LaunchedEffect(graph, viewport) { if (viewport.width > 0 && viewport.height > 0) fit() }
    Column(modifier.inspectElement("NODES_CONTENT", "Graph toolbar, interactive canvas, and gesture hint")) {
        Row(Modifier.fillMaxWidth().inspectElement("NODES_TOOLBAR", "Fit, find, locate playing, and graph controls"), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { fit() }, modifier = Modifier.inspectElement("NODES_FIT_BUTTON", "Fits all graph nodes on screen")) { Text("Fit graph") }
            TextButton(onClick = { picker = true }, modifier = Modifier.inspectElement("NODES_FIND_BUTTON", "Find media by filename")) { Text("Find") }
            TextButton(onClick = {
                if (current >= 0) { scale = 2f; pan = Offset(-graph.points[current].x * scale, -graph.points[current].y * scale) }
            }, enabled = current >= 0, modifier = Modifier.inspectElement("NODES_PLAYING_BUTTON", "Centers currently playing media")) { Text("Playing") }
            TextButton(onClick = { controls = true }, modifier = Modifier.inspectElement("NODES_CONTROLS_BUTTON", "Graph display and connection settings")) { Text("Controls") }
        }
        Canvas(Modifier.fillMaxWidth().weight(1f).inspectElement("NODES_GRAPH_CANVAS", "Pan, zoom, drag a node, or tap it to play")
            .onGloballyPositioned { canvasBounds = it.boundsInRoot() }.onSizeChanged { viewport = it }
            .semantics { contentDescription = "Filename similarity graph. Pan or pinch to explore, drag nodes, tap to play. Use Find / play a node for a text list." }
            .pointerInput(graph, presentation.visibleNodes, current) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Keep the system-like edge swipe available for Nodes → Library;
                    // the graph owns drags started farther inside its canvas.
                    if (down.position.x < 56.dp.toPx()) return@awaitEachGesture
                    down.consume()
                    val center = Offset(viewport.width / 2f, viewport.height / 2f)
                    fun position(i: Int) = moved[i] ?: Offset(graph.points[i].x, graph.points[i].y)
                    val hit = (presentation.visibleNodes + listOfNotNull(current.takeIf { it >= 0 })).minByOrNull { (position(it) * scale + pan + center - down.position).getDistanceSquared() }
                        ?.takeIf { (position(it) * scale + pan + center - down.position).getDistance() < 28.dp.toPx() }
                    var travelled = 0f
                    var multi = false
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val delta = event.calculatePan()
                        multi = multi || event.changes.count { it.pressed } > 1
                        travelled += delta.getDistance()
                        if (multi) {
                            val old = scale
                            scale = (scale * zoom).coerceIn(0.08f, 8f)
                            val pointers = event.changes.filter { it.pressed }
                            val focus = if (pointers.isEmpty()) center else pointers.map { it.position }.reduce(Offset::plus) / pointers.size.toFloat()
                            pan = (pan - (focus - center)) * (scale / old) + (focus - center) + delta
                        } else if (travelled > viewConfiguration.touchSlop) {
                            if (hit != null) { moved[hit] = position(hit) + delta / scale; selected = hit }
                            else pan += delta
                        }
                        if (multi || travelled > viewConfiguration.touchSlop) event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    if (!multi && travelled <= viewConfiguration.touchSlop && hit != null) {
                        selected = hit; onPlayCurrent(graph.nodes[hit].id)
                    }
                }
            }) {
            val center = Offset(size.width / 2, size.height / 2)
            val positions = graph.points.mapIndexed { i, p -> (moved[i] ?: Offset(p.x, p.y)) * scale + pan + center }
            fun visible(p: Offset) = p.x in -40f..size.width + 40 && p.y in -40f..size.height + 40
            presentation.edges.forEach { edge ->
                val a = positions[edge.a]; val b = positions[edge.b]
                val connected = edge.a == current || edge.b == current || edge.a == selected || edge.b == selected
                if (visible(a) || visible(b)) drawLine((if (connected) color else ink).copy(alpha =
                    (options.edgeOpacity * (if (connected) 1.4f else .65f) * (.35f + edge.strength)).coerceIn(.02f, 1f)), a, b,
                    (0.45f + edge.strength * (if (connected) 1.5f else .75f)) * density)
            }
            graph.nodes.forEachIndexed { i, node ->
                val p = positions[i]
                if (visible(p) && (i in presentation.visibleNodes || i == current)) {
                    val playing = i == current
                    val active = playing || i == selected
                    val importance = if (options.sizeByConnections) presentation.importance[i] else 0f
                    val radius = ((3.5f + importance * 5.5f) * options.nodeSize).dp.toPx()
                    if (active) drawCircle(color.copy(alpha = .14f), radius + 8.dp.toPx(), p)
                    drawCircle(if (active) color else ink.copy(alpha = .48f + importance * .4f), radius, p)
                    if (playing) {
                        drawCircle(color, radius + 4.dp.toPx(), p, style = Stroke(1.5.dp.toPx()))
                        drawPath(Path().apply { moveTo(p.x-2.dp.toPx(), p.y-3.dp.toPx()); lineTo(p.x+3.dp.toPx(),p.y); lineTo(p.x-2.dp.toPx(),p.y+3.dp.toPx()); close() }, onAccent)
                    }
                }
            }
            // Dense libraries need a few useful labels, not hundreds of overlapping
            // filenames. Priority goes to playing/selected nodes, then strong hubs.
            val labelCandidates = (listOf(current, selected) +
                presentation.visibleNodes.sortedByDescending { presentation.importance[it] })
                .distinct().filter { it in graph.nodes.indices && visible(positions[it]) }
                .take(if (options.showLabels && scale > .8f) 10 else 2)
            val occupied = mutableListOf<android.graphics.RectF>()
            labelCandidates.forEach { i ->
                val p = positions[i]
                val importance = presentation.importance[i]
                val radius = ((3.5f + importance * 5.5f) * options.nodeSize).dp.toPx()
                val raw = graph.nodes[i].filename.substringBeforeLast('.').take(28)
                labelPaint.textSize = 11.dp.toPx()
                labelPaint.isFakeBoldText = i == current || i == selected
                val text = if (raw.length == 28) "$raw…" else raw
                val textWidth = labelPaint.measureText(text)
                val pad = 5.dp.toPx()
                val x = (if (p.x + radius + textWidth + 3 * pad > size.width)
                    p.x - radius - textWidth - 3 * pad else p.x + radius + pad)
                    .coerceIn(pad, (size.width - textWidth - 2 * pad).coerceAtLeast(pad))
                val box = android.graphics.RectF(x, p.y - 12.dp.toPx(), x + textWidth + 2 * pad, p.y + 8.dp.toPx())
                if (occupied.none { android.graphics.RectF.intersects(it, box) }) {
                    occupied.add(box)
                    drawIntoCanvas {
                        it.nativeCanvas.drawRoundRect(box, 4.dp.toPx(), 4.dp.toPx(), backingPaint)
                        it.nativeCanvas.drawText(text, x + pad, p.y + 4.dp.toPx(), labelPaint)
                    }
                }
            }
        }
        Text("Pinch to zoom · Drag to move · Tap to play", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)
                .inspectElement("NODES_GESTURE_HINT", "Graph gesture instructions"))
    }
    if (controls) GraphControls(options, { onOptions(it); controls = false }, { controls = false })
    if (picker) AlertDialog(onDismissRequest = { picker = false }, title = { Text("Find a node") }, text = {
        Column {
            OutlinedTextField(query, { query = it }, label = { Text("Filename") }, singleLine = true)
            val found = remember(graph, query) { graph.nodes.filter { it.filename.contains(query, ignoreCase = true) } }
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(found, key = { it.id }) { node -> TextButton(onClick = { picker = false; onPlay(node.id) }) { Text(node.filename) } }
            }
        }
    }, confirmButton = { TextButton(onClick = { picker = false }) { Text("Close") } })
}

@Composable
private fun GraphControls(options: GraphOptions, onSave: (GraphOptions) -> Unit, onClose: () -> Unit) {
    var edit by remember { mutableStateOf(options) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Graph controls") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Connection strength · ${(edit.threshold * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            Slider(edit.threshold, { edit = edit.copy(threshold = it) }, valueRange = 0f..1f)
            Text("Node size", style = MaterialTheme.typography.labelLarge)
            Slider(edit.nodeSize, { edit = edit.copy(nodeSize = it) }, valueRange = .6f..2f)
            Text("Link visibility", style = MaterialTheme.typography.labelLarge)
            Slider(edit.edgeOpacity, { edit = edit.copy(edgeOpacity = it) }, valueRange = .05f..1f)
            GraphToggle("Filename labels", edit.showLabels) { edit = edit.copy(showLabels = it) }
            GraphToggle("Hide isolated nodes", edit.hideIsolated) { edit = edit.copy(hideIsolated = it) }
            GraphToggle("Size by strong connections", edit.sizeByConnections) { edit = edit.copy(sizeByConnections = it) }
            Text("The double-ring node is playing. Larger nodes have more strong filename connections.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { edit = GraphOptions() }) { Text("Restore graph defaults") }
        }
    }, confirmButton = { TextButton(onClick = { onSave(edit) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } })
}

@Composable
private fun GraphToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, onChange)
    }
}
