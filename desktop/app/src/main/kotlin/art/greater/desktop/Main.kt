package art.greater.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import java.awt.BorderLayout
import javax.swing.*
import java.nio.file.Path
import kotlin.math.*

fun main(args: Array<String>) {
    if (args.firstOrNull() == "--backend-probe") {
        runNativeProbe(args.drop(1).toTypedArray())
    } else desktopApplication(args)
}

private fun desktopApplication(args: Array<String>) = application {
    val session = remember { runCatching { DesktopSession(DesktopStore(), NativePlayback()) } }
    val windowState = rememberWindowState(width = 1360.dp, height = 880.dp)
    Window(onCloseRequest = { session.getOrNull()?.close(); exitApplication() }, title = "Greater Art Desktop · 0.1.0 development", state = windowState) {
        MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xff94d7c3), background = Color(0xff101715), surface = Color(0xff192320))) {
            Surface(Modifier.fillMaxSize()) {
                session.fold(onSuccess = { value ->
                    LaunchedEffect(Unit) {
                        if (args.size == 2 && args[0] == "--open") {
                            val path = Path.of(args[1]); value.play(Media(path.toString(), path.fileName.toString(), java.nio.file.Files.size(path), 0))
                        }
                    }
                    Workspace(value)
                }, onFailure = { error -> Column(Modifier.padding(32.dp)) {
                    Text("Greater Art", style = MaterialTheme.typography.headlineLarge)
                    Text(error.message ?: "Native runtime unavailable")
                    Text("No playback is simulated. Install the development runtime described in desktop/README.md.")
                } })
            }
        }
    }
}

@Composable private fun Workspace(session: DesktopSession) {
    val state by session.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var left by remember { mutableStateOf(session.store.pane("pane.left", .28f)) }
    var right by remember { mutableStateOf(session.store.pane("pane.right", .28f)) }
    val filtered = remember(state.library, query, onlyFavorites, state.favorites) { state.library.filter {
        it.name.contains(query, true) && (!onlyFavorites || it.id in state.favorites)
    } }
    fun savePanes() { session.store.values["pane.left"] = left.toString(); session.store.values["pane.right"] = right.toString(); session.store.save() }
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Greater Art", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            Text(if (state.busy) "Indexing…" else "${state.library.size} local files", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { chooseFolder()?.let { session.roots(state.roots + it) } }) { Text("Add folder") }
            TextButton(onClick = session::rescan, enabled = !state.busy) { Text("Rescan") }
        }
        state.error?.let { error -> Row { Text(error, color = MaterialTheme.colorScheme.error); TextButton(onClick = session::clearError) { Text("Dismiss") } } }
        BoxWithConstraints(Modifier.weight(1f)) {
            val available = (maxWidth.value - 20f).coerceAtLeast(1050f)
            Row(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                Pane("STACK", Modifier.width((available * left).dp)) {
                    state.current?.let { Text("PRIMARY · ${it.name}", style = MaterialTheme.typography.titleMedium) }
                    Text("Add a source with + in Library. Up to eight sources.", style = MaterialTheme.typography.bodySmall)
                    LazyColumn(Modifier.weight(1f)) { items(state.voices, key = { it.player.toString() }) { voice ->
                        Column(Modifier.padding(vertical = 10.dp)) {
                            Text(voice.media.name, maxLines = 2)
                            Row { FilterChip(voice.muted, { session.voice(voice, mute = !voice.muted) }, { Text("Mute") }); Spacer(Modifier.width(6.dp)); FilterChip(voice.solo, { session.voice(voice, solo = !voice.solo) }, { Text("Solo") }); TextButton({ session.removeVoice(voice) }) { Text("Remove") } }
                            Slider(voice.gain.toFloat(), { session.voice(voice, gain = it.toInt()) }, valueRange = 0f..100f)
                        }
                    } }
                    Text("Alignment and saved-Stack parity are not implemented in 0.1.0.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DividerHandle({ delta -> left = (left + delta / available).coerceIn(.18f, .4f) }, ::savePanes)
                Pane("LIBRARY", Modifier.width((available * (1f - left - right)).coerceAtLeast(310f).dp)) {
                    if (!expanded) VideoPreview(session, Modifier.fillMaxWidth().height(180.dp))
                    OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("Search library") }, modifier = Modifier.fillMaxWidth())
                    Row { FilterChip(!onlyFavorites, { onlyFavorites = false }, { Text("All songs") }); Spacer(Modifier.width(8.dp)); FilterChip(onlyFavorites, { onlyFavorites = true }, { Text("Favorites") }) }
                    LazyColumn(Modifier.weight(1f)) { items(filtered, key = { it.id }) { media ->
                        Row(Modifier.fillMaxWidth().background(if (media.id == state.current?.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable { session.play(media) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(media.name, maxLines = 2, modifier = Modifier.weight(1f))
                            TextButton({ session.favorite(media) }) { Text(if (media.id in state.favorites) "♥" else "♡") }
                            TextButton({ session.addVoice(media) }, enabled = state.current != null && state.voices.size < 7) { Text("+") }
                        }
                    } }
                    if (state.roots.isEmpty()) Text("Select a media folder. Nothing is scanned without your choice.")
                }
                DividerHandle({ delta -> right = (right - delta / available).coerceIn(.18f, .4f) }, ::savePanes)
                Pane("NODES", Modifier.width((available * right).dp)) {
                    Text("Filename similarity · real library graph", style = MaterialTheme.typography.bodySmall)
                    val graph = state.graph
                    val degrees = remember(graph) {
                        IntArray(graph?.nodes?.size ?: 0).also { counts ->
                            graph?.edges?.forEach { counts[it.a]++; counts[it.b]++ }
                        }
                    }
                    var zoom by remember { mutableStateOf(1f) }
                    Slider(zoom, { zoom = it }, valueRange = .5f..3f)
                    Canvas(Modifier.fillMaxWidth().weight(1f)) {
                        if (graph != null && graph.points.isNotEmpty()) {
                            val minX = graph.points.minOf { it.x }; val maxX = graph.points.maxOf { it.x }
                            val minY = graph.points.minOf { it.y }; val maxY = graph.points.maxOf { it.y }
                            fun point(i: Int): Offset { val p = graph.points[i]; return Offset(size.width * (.5f + ((p.x - minX) / (maxX - minX).coerceAtLeast(1f) - .5f) * .85f * zoom), size.height * (.5f + ((p.y - minY) / (maxY - minY).coerceAtLeast(1f) - .5f) * .85f * zoom)) }
                            graph.edges.forEach { drawLine(Color(0xff83ad9c).copy(alpha = .1f + it.strength * .5f), point(it.a), point(it.b)) }
                            graph.points.indices.forEach { i -> drawCircle(if (graph.nodes[i].id == state.current?.id) Color(0xff94d7c3) else Color(0xffbdcbc5), 4f + degrees[i].coerceAtMost(20) * .4f, point(i)) }
                        }
                    }
                }
            }
        }
        Transport(session, state, { expanded = true })
    }
    if (expanded) Window({ expanded = false }, title = "Greater Art · Now Playing", state = rememberWindowState(width = 960.dp, height = 650.dp)) {
        MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xff94d7c3))) {
            Column(Modifier.fillMaxSize().background(Color.Black)) {
                VideoPreview(session, Modifier.fillMaxWidth().weight(1f))
                Surface { Transport(session, state, { window.extendedState = JFrame.MAXIMIZED_BOTH }) }
            }
        }
    }
}

@Composable private fun VideoPreview(session: DesktopSession, modifier: Modifier) {
    SwingPanel(factory = { JPanel(BorderLayout()).also { panel ->
        val canvas = object : java.awt.Canvas() { override fun addNotify() { super.addNotify(); session.attach(this) } }.apply { background = java.awt.Color.BLACK }
        panel.add(canvas, BorderLayout.CENTER)
    } }, modifier = modifier)
}

@Composable private fun Pane(title: String, modifier: Modifier, body: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.surface.copy(alpha = .7f)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); body() }
    }
}
@Composable private fun DividerHandle(onDrag: (Float) -> Unit, onEnd: () -> Unit) {
    val density = LocalDensity.current.density
    val currentDrag by rememberUpdatedState(onDrag)
    val currentEnd by rememberUpdatedState(onEnd)
    Box(Modifier.width(10.dp).fillMaxHeight().pointerInput(density) {
        detectDragGestures(onDragEnd = { currentEnd() }) { change, amount ->
            change.consume(); currentDrag(amount.x / density)
        }
    }.background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = .3f)))
}
@Composable private fun Transport(session: DesktopSession, state: DesktopState, onExpand: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(state.current?.name ?: "Greater Art · local playback", maxLines = 1)
        Slider(state.playback.time.coerceAtMost(state.playback.duration).toFloat(), { session.seek(it.toLong()) }, valueRange = 0f..state.playback.duration.coerceAtLeast(1).toFloat(), enabled = state.current != null)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton({ session.next(-1) }) { Text("Previous") }
            Button(session::toggle, enabled = state.current != null) { Text(if (state.playback.playing) "Pause" else "Play") }
            TextButton({ session.next(1) }) { Text("Next") }
            TextButton(session::stop) { Text("Stop") }
            TextButton(session::repeat) { Text(if (state.repeat) "Loop on" else "Loop off") }
            TextButton({ session.speed(if (state.speed >= 2f) .5f else state.speed + .25f) }) { Text("${state.speed}×") }
            Slider(state.volume.toFloat(), { session.volume(it.toInt()) }, valueRange = 0f..100f, modifier = Modifier.width(160.dp))
            Spacer(Modifier.weight(1f)); Text("${time(state.playback.time)} / ${time(state.playback.duration)}")
            TextButton(onExpand) { Text("Expand") }
        }
    }
}
private fun time(value: Long) = "%d:%02d".format(value / 60000, value / 1000 % 60)
private fun chooseFolder(): Path? {
    val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile.toPath() else null
}
