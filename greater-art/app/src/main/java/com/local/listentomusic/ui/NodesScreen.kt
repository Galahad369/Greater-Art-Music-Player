package com.local.listentomusic.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
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
import com.local.listentomusic.ui.components.GaChromeSurface
import com.local.listentomusic.ui.theme.GaControl
import com.local.listentomusic.ui.theme.GaRadius
import com.local.listentomusic.ui.theme.GaSpacing
import kotlin.math.*

/** Nodes is the right-hand page of the persistent Stack | All songs | Nodes navigator. */
@Composable
fun NodesScreen(graph: LibraryGraph?, loading: Boolean, error: String?, currentPath: String?,
    contentPadding: PaddingValues, onRetry: () -> Unit, onPlay: (String) -> Unit,
    options: GraphOptions, onOptions: (GraphOptions) -> Unit) {
    Column(Modifier.fillMaxSize().inspectElement("NODES_SCREEN", "Filename-similarity graph and controls")
        .padding(contentPadding).consumeWindowInsets(contentPadding)) {
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
    val focusIndex = selected.takeIf { it >= 0 } ?: current.takeIf { it >= 0 } ?: -1
    val focusNeighbors = remember(presentation.edges, focusIndex) {
        if (focusIndex < 0) emptySet() else presentation.edges.asSequence().mapNotNull { edge ->
            when (focusIndex) {
                edge.a -> edge.b
                edge.b -> edge.a
                else -> null
            }
        }.toSet()
    }
    var picker by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val color = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val ink = MaterialTheme.colorScheme.onSurface
    val inspectorDensity = LocalDensity.current
    val labelTextSize = with(inspectorDensity) { MaterialTheme.typography.labelMedium.fontSize.toPx() }
    val labelPaint = remember(ink, labelTextSize) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = ink.toArgb()
            textSize = labelTextSize
        }
    }
    val labelBacking = MaterialTheme.colorScheme.surface.copy(alpha = .88f)
    val backingPaint = remember(labelBacking) { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = labelBacking.toArgb()
    } }
    val onPlayCurrent by rememberUpdatedState(onPlay)
    val inspector = LocalUiInspector.current
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
        GaChromeSurface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = GaSpacing.lg, vertical = GaSpacing.sm)
                .inspectElement("NODES_TOOLBAR", "Fit, find, locate playing, and graph controls"),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = GaSpacing.xs, vertical = GaSpacing.xs),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                GraphTool(Icons.Rounded.CenterFocusStrong, "Fit", "NODES_FIT_BUTTON") { fit() }
                GraphTool(Icons.Rounded.Search, "Find", "NODES_FIND_BUTTON") { picker = true }
                GraphTool(Icons.Rounded.MyLocation, "Playing", "NODES_PLAYING_BUTTON", enabled = current >= 0) {
                    if (current >= 0) { scale = 2f; pan = Offset(-graph.points[current].x * scale, -graph.points[current].y * scale) }
                }
                GraphTool(Icons.Rounded.Tune, "Controls", "NODES_CONTROLS_BUTTON") { controls = true }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = GaSpacing.lg, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .44f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .22f)),
                tonalElevation = 0.dp,
            ) {
                Text(
                    "${graph.nodes.size} nodes · ${presentation.edges.size} links",
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            current.takeIf { it >= 0 }?.let { index ->
                Spacer(Modifier.width(8.dp))
                Text(
                    "Playing · ${graph.nodes[index].filename.substringBeforeLast('.').take(34)}",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Canvas(Modifier.fillMaxWidth().weight(1f).inspectElement("NODES_GRAPH_CANVAS", "Pinch to zoom, drag nodes, tap to play; empty-space swipes navigate pages")
            .onGloballyPositioned { canvasBounds = it.boundsInRoot() }.onSizeChanged { viewport = it }
            .semantics { contentDescription = "Filename similarity graph. Pinch to zoom, drag individual nodes, tap a node to play, or swipe empty space to change page. Use Find / play a node for a text list." }
            .pointerInput(graph, presentation.visibleNodes, current) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(viewport.width / 2f, viewport.height / 2f)
                    fun position(i: Int) = moved[i] ?: Offset(graph.points[i].x, graph.points[i].y)
                    val hit = (presentation.visibleNodes + listOfNotNull(current.takeIf { it >= 0 })).minByOrNull {
                        (position(it) * scale + pan + center - down.position).getDistanceSquared()
                    }?.takeIf {
                        (position(it) * scale + pan + center - down.position).getDistance() < 28.dp.toPx()
                    }

                    // Single-finger ownership is intentionally conditional:
                    // - direct node touch -> Nodes consumes it for tap/drag;
                    // - empty canvas -> leave it unconsumed so HorizontalPager can
                    //   swipe Nodes <-> All songs from anywhere on the empty map.
                    if (hit != null) down.consume()

                    var travelled = 0f
                    var multi = false
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        multi = multi || pressedCount > 1

                        if (multi) {
                            // Two fingers belong to graph zoom. There is deliberately
                            // no user-driven map translation: preserve the currently
                            // centered graph point while scale changes.
                            if (pressedCount > 1) {
                                val oldScale = scale
                                val newScale = (oldScale * event.calculateZoom()).coerceIn(0.08f, 8f)
                                if (newScale != oldScale) {
                                    pan *= newScale / oldScale
                                    scale = newScale
                                }
                            }
                            // Once a pinch begins, keep the whole gesture away from
                            // the pager and node-tap handlers until all pointers lift.
                            event.changes.forEach { it.consume() }
                        } else if (hit != null) {
                            val delta = event.calculatePan()
                            travelled += delta.getDistance()
                            if (travelled > viewConfiguration.touchSlop) {
                                moved[hit] = position(hit) + delta / scale
                                selected = hit
                                event.changes.forEach { it.consume() }
                            }
                        }
                        // Empty single-finger gestures are never consumed here.
                        // The parent HorizontalPager therefore owns horizontal swipes.
                    } while (event.changes.any { it.pressed })

                    if (!multi && travelled <= viewConfiguration.touchSlop && hit != null) {
                        selected = hit
                        onPlayCurrent(graph.nodes[hit].id)
                    }
                }
            }) {
            val center = Offset(size.width / 2, size.height / 2)
            val positions = graph.points.mapIndexed { i, p -> (moved[i] ?: Offset(p.x, p.y)) * scale + pan + center }
            fun visible(p: Offset) = p.x in -40f..size.width + 40 && p.y in -40f..size.height + 40

            val gridStep = 32.dp.toPx()
            var gx = (pan.x % gridStep + gridStep) % gridStep
            while (gx < size.width) {
                var gy = (pan.y % gridStep + gridStep) % gridStep
                while (gy < size.height) {
                    drawCircle(ink.copy(alpha = .055f), 0.9.dp.toPx(), Offset(gx, gy))
                    gy += gridStep
                }
                gx += gridStep
            }

            presentation.edges.forEach { edge ->
                val a = positions[edge.a]; val b = positions[edge.b]
                val connected = edge.a == current || edge.b == current || edge.a == selected || edge.b == selected
                val focusFactor = if (focusIndex >= 0 && !connected) .30f else 1f
                if (visible(a) || visible(b)) drawLine((if (connected) color else ink).copy(alpha =
                    (options.edgeOpacity * focusFactor * (if (connected) 1.4f else .65f) * (.35f + edge.strength)).coerceIn(.015f, 1f)), a, b,
                    (0.45f + edge.strength * (if (connected) 1.5f else .75f)) * density)
            }
            graph.nodes.forEachIndexed { i, node ->
                val p = positions[i]
                if (visible(p) && (i in presentation.visibleNodes || i == current)) {
                    val playing = i == current
                    val active = playing || i == selected
                    val related = focusIndex < 0 || i == focusIndex || i in focusNeighbors || active
                    val importance = if (options.sizeByConnections) presentation.importance[i] else 0f
                    val radius = ((3.5f + importance * 5.5f) * options.nodeSize).dp.toPx()
                    if (active) {
                        drawCircle(
                            color.copy(alpha = .38f),
                            radius + 7.dp.toPx(),
                            p,
                            style = Stroke(1.1.dp.toPx()),
                        )
                    }
                    drawCircle(if (active) color else ink.copy(alpha = if (related) .48f + importance * .4f else .18f), radius, p)
                    if (playing) {
                        drawCircle(color, radius + 4.dp.toPx(), p, style = Stroke(1.8.dp.toPx()))
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
                labelPaint.textSize = labelTextSize
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
        selected.takeIf { it in graph.nodes.indices }?.let { index ->
            val linked = presentation.edges.filter { it.a == index || it.b == index }
            val strongest = linked.sortedByDescending { it.strength }.take(2).map { edge ->
                graph.nodes[if (edge.a == index) edge.b else edge.a].filename.substringBeforeLast('.')
            }
            GaChromeSurface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = GaSpacing.lg, vertical = 2.dp)
                    .inspectElement("NODES_SELECTED_CARD", graph.nodes[index].filename),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            graph.nodes[index].filename.substringBeforeLast('.'),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            buildString {
                                append("${linked.size} links")
                                if (strongest.isNotEmpty()) append(" · ").append(strongest.joinToString(" · "))
                            },
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (index == current) {
                        Text("PLAYING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    } else {
                        TextButton(onClick = { onPlayCurrent(graph.nodes[index].id) }) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Play")
                        }
                    }
                }
            }
        }
        Text(
            "Tap node to play  •  Drag node  •  Pinch to zoom  •  Swipe empty space to navigate",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = GaSpacing.lg, vertical = GaSpacing.sm)
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
private fun GraphTool(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, element: String,
    enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.widthIn(min = 68.dp).heightIn(min = GaControl.touchTarget)
            .inspectElement(element, label),
        contentPadding = PaddingValues(horizontal = GaSpacing.xs, vertical = GaSpacing.xs),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(21.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun GraphControls(options: GraphOptions, onSave: (GraphOptions) -> Unit, onClose: () -> Unit) {
    var edit by remember { mutableStateOf(options) }
    AlertDialog(onDismissRequest = onClose,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Graph controls", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Obsidian-style display and forces", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close") }
            }
        }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GraphControlSection("Connections") {
                    GraphSlider("Minimum similarity", "${(edit.threshold * 100).toInt()}%", edit.threshold, 0f..1f) { edit = edit.copy(threshold = it) }
                    GraphSlider("Link visibility", "${(edit.edgeOpacity * 100).toInt()}%", edit.edgeOpacity, .05f..1f) { edit = edit.copy(edgeOpacity = it) }
                    GraphToggle("Hide isolated nodes", edit.hideIsolated) { edit = edit.copy(hideIsolated = it) }
                }
                GraphControlSection("Nodes") {
                    GraphSlider("Node size", "%.1f×".format(edit.nodeSize), edit.nodeSize, .6f..2f) { edit = edit.copy(nodeSize = it) }
                    GraphToggle("Filename labels", edit.showLabels) { edit = edit.copy(showLabels = it) }
                    GraphToggle("Size by strong connections", edit.sizeByConnections) { edit = edit.copy(sizeByConnections = it) }
                }
                Text("The double-ring node is playing. Strongly connected files appear larger and closer.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { edit = GraphOptions() }, modifier = Modifier.fillMaxWidth()) { Text("Restore graph defaults") }
            }
        }, confirmButton = { Button(onClick = { onSave(edit) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } })
}

@Composable
private fun GraphControlSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(GaRadius.control),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .44f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .24f)),
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(7.dp))
            content()
        }
    }
}

@Composable
private fun GraphSlider(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
    }
    Slider(value, onChange, valueRange = range)
}

@Composable
private fun GraphToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, onChange)
    }
}
