package art.greater.desktop

import java.awt.*
import java.nio.file.*
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.imageio.ImageIO

/** Opt-in real local-media backend proof; no user output capture or network source. */
fun main(args: Array<String>) {
    runNativeProbe(args)
}

fun runNativeProbe(args: Array<String>) {
    require(args.size == 1 && Files.isRegularFile(Path.of(args[0]))) { "Pass an owned local fixture using -Pfixture=<path>" }
    val engine = NativePlayback()
    val primary = engine.create()
    val companion = engine.create()
    val frame = JFrame("Greater Art native backend proof")
    val canvas = Canvas().apply { background = Color.BLACK }
    try {
        SwingUtilities.invokeAndWait { frame.add(canvas); frame.setSize(640, 420); frame.isVisible = true; engine.attach(primary, canvas) }
        engine.load(primary, args[0]); engine.play(primary)
        fun waitFor(check: () -> Boolean) { val until = System.nanoTime() + 12_000_000_000L
            while (!check() && System.nanoTime() < until) Thread.sleep(50)
            check(check()) { "Native state deadline missed" }
        }
        waitFor { engine.snapshot(primary).playing && engine.snapshot(primary).time > 300 }
        val before = engine.snapshot(primary)
        check(before.duration > 0)
        val evidence = Path.of("build", "native-probe").toAbsolutePath()
        Files.createDirectories(evidence)
        fun checkFrame(name: String) {
            val output = evidence.resolve("$name.png")
            waitFor { engine.hasVideo(primary) }
            check(engine.snapshotVideo(primary, output)) { "Native frame snapshot failed" }
            waitFor { Files.isRegularFile(output) && Files.size(output) > 0 }
            val image = requireNotNull(ImageIO.read(output.toFile()))
            check(image.width > 0 && image.height > 0)
            check((image.getRGB(image.width / 2, image.height / 2) and 0xffffff) != 0) { "Fixture frame is unexpectedly black" }
            println("decodedFrame=$name size=${image.width}x${image.height} path=$output")
        }
        checkFrame("before-seek")
        engine.pause(primary, true); Thread.sleep(200)
        check(!engine.snapshot(primary).playing)
        engine.seek(primary, 2000); engine.pause(primary, false)
        waitFor { engine.snapshot(primary).playing && engine.snapshot(primary).time >= 2000 }
        checkFrame("after-seek")
        check(engine.speed(primary, 1.25f)); engine.volume(primary, 55)
        engine.load(companion, args[0], audioOnly = true); engine.volume(companion, 20); engine.play(companion)
        waitFor { engine.snapshot(companion).playing }
        Thread.sleep(250)
        check(!engine.hasVideo(companion)) { "Audio companion unexpectedly created a video output" }
        println("backend=${engine.version()} primaryPlaying=true companionPlaying=true duration=${before.duration} seekPauseResume=true video=${engine.hasVideo(primary)}")
        SwingUtilities.invokeAndWait { frame.extendedState = JFrame.ICONIFIED }
        Thread.sleep(400); check(engine.snapshot(primary).playing)
        println("minimizedContinues=true")
    } finally { engine.close(); SwingUtilities.invokeAndWait { frame.dispose() }; check(engine.closed); println("nativeClosed=true") }
}
