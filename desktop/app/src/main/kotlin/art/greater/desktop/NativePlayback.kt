package art.greater.desktop

import com.sun.jna.*
import java.awt.Canvas
import java.nio.file.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Direct C boundary avoids adopting vlcj's GPL wrapper without a licensing decision. */
interface Vlc : Library {
    fun libvlc_new(count: Int, options: Array<String>): Pointer?
    fun libvlc_release(instance: Pointer)
    fun libvlc_media_new_path(instance: Pointer, path: String): Pointer?
    fun libvlc_media_release(media: Pointer)
    fun libvlc_media_add_option(media: Pointer, option: String)
    fun libvlc_media_player_new(instance: Pointer): Pointer?
    fun libvlc_media_player_release(player: Pointer)
    fun libvlc_media_player_set_media(player: Pointer, media: Pointer)
    fun libvlc_media_player_set_hwnd(player: Pointer, hwnd: Pointer?)
    fun libvlc_media_player_play(player: Pointer): Int
    fun libvlc_media_player_set_pause(player: Pointer, pause: Int)
    fun libvlc_media_player_stop(player: Pointer)
    fun libvlc_media_player_get_time(player: Pointer): Long
    fun libvlc_media_player_set_time(player: Pointer, time: Long)
    fun libvlc_media_player_get_length(player: Pointer): Long
    fun libvlc_media_player_is_playing(player: Pointer): Int
    fun libvlc_media_player_get_state(player: Pointer): Int
    fun libvlc_media_player_set_rate(player: Pointer, rate: Float): Int
    fun libvlc_audio_set_volume(player: Pointer, volume: Int): Int
    fun libvlc_media_player_has_vout(player: Pointer): Int
    fun libvlc_video_take_snapshot(player: Pointer, number: Int, path: String, width: Int, height: Int): Int
    fun libvlc_get_version(): String
}

data class Playback(val time: Long = 0, val duration: Long = 0, val playing: Boolean = false, val state: Int = 0)

class NativePlayback(runtime: Path = locateRuntime()) : AutoCloseable {
    val executor = Executors.newSingleThreadScheduledExecutor { runnable -> Thread(runnable, "greater-art-native").apply { isDaemon = true } }
    private val vlc: Vlc
    private val instance: Pointer
    private val players = mutableListOf<Pointer>()
    @Volatile var closed = false; private set
    init {
        require(Files.isRegularFile(runtime.resolve("libvlc.dll"))) { "Bundled libVLC runtime is unavailable" }
        NativeLibrary.addSearchPath("libvlc", runtime.toString())
        NativeLibrary.addSearchPath("libvlccore", runtime.toString())
        System.setProperty("jna.encoding", "UTF-8")
        vlc = Native.load(runtime.resolve("libvlc.dll").toString(), Vlc::class.java)
        // VLC resolves its adjacent plugins directory. --plugin-path was removed in VLC 3.
        val options = arrayOf("--no-video-title-show", "--no-osd", "--no-metadata-network-access", "--no-lua", "--audio-time-stretch")
        instance = requireNotNull(vlc.libvlc_new(options.size, options)) { "libVLC initialization failed" }
    }
    fun version() = vlc.libvlc_get_version()
    fun create(): Pointer = requireNotNull(vlc.libvlc_media_player_new(instance)).also(players::add)
    fun load(player: Pointer, path: String, audioOnly: Boolean = false) {
        val file = Path.of(path).toAbsolutePath().normalize()
        require(Files.isRegularFile(file) && Files.isReadable(file)) { "Media is missing or inaccessible" }
        val media = requireNotNull(vlc.libvlc_media_new_path(instance, file.toString()))
        try {
            // Companion audio must not spawn a separate native video window.
            // This never changes the primary's video or either voice's source audio quality.
            if (audioOnly) vlc.libvlc_media_add_option(media, ":no-video")
            vlc.libvlc_media_player_set_media(player, media)
        } finally { vlc.libvlc_media_release(media) }
    }
    fun attach(player: Pointer, canvas: Canvas) { check(canvas.isDisplayable); vlc.libvlc_media_player_set_hwnd(player, Native.getComponentPointer(canvas)) }
    fun play(player: Pointer) { check(vlc.libvlc_media_player_play(player) == 0) { "Native playback could not start" } }
    fun pause(player: Pointer, paused: Boolean) = vlc.libvlc_media_player_set_pause(player, if (paused) 1 else 0)
    fun stop(player: Pointer) = vlc.libvlc_media_player_stop(player)
    fun release(player: Pointer) { if (players.remove(player)) { vlc.libvlc_media_player_stop(player); vlc.libvlc_media_player_release(player) } }
    fun seek(player: Pointer, milliseconds: Long) = vlc.libvlc_media_player_set_time(player, milliseconds.coerceAtLeast(0))
    fun volume(player: Pointer, percent: Int) = vlc.libvlc_audio_set_volume(player, percent.coerceIn(0, 100))
    fun speed(player: Pointer, speed: Float): Boolean = speed.isFinite() && speed in .25f..4f && vlc.libvlc_media_player_set_rate(player, speed) == 0
    fun snapshot(player: Pointer) = Playback(vlc.libvlc_media_player_get_time(player).coerceAtLeast(0), vlc.libvlc_media_player_get_length(player).coerceAtLeast(0), vlc.libvlc_media_player_is_playing(player) != 0, vlc.libvlc_media_player_get_state(player))
    fun hasVideo(player: Pointer) = vlc.libvlc_media_player_has_vout(player) > 0
    fun snapshotVideo(player: Pointer, path: Path) = vlc.libvlc_video_take_snapshot(player, 0, path.toAbsolutePath().toString(), 0, 0) == 0
    override fun close() {
        if (closed) return
        closed = true
        val cleanup = executor.submit { players.forEach { vlc.libvlc_media_player_stop(it); vlc.libvlc_media_player_release(it) }; players.clear(); vlc.libvlc_release(instance) }
        executor.shutdown()
        cleanup.get(10, TimeUnit.SECONDS)
    }
    companion object {
        fun locateRuntime(): Path {
            val candidates = listOfNotNull(System.getProperty("greaterart.vlc"), System.getenv("GREATER_ART_VLC"), System.getProperty("compose.application.resources.dir")?.let { "$it/vlc" })
            return candidates.map(Path::of).firstOrNull { Files.isRegularFile(it.resolve("libvlc.dll")) }
                ?: throw IllegalStateException("No bundled runtime. Development: set GREATER_ART_VLC to the reviewed VLC folder.")
        }
    }
}
