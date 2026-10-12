package art.greater.desktop

import java.nio.file.*
import java.util.Locale
import java.util.Properties

data class Media(val id: String, val name: String, val bytes: Long, val modified: Long)

object LocalLibrary {
    private val extensions = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "mp4", "mkv", "webm", "avi", "mov", "m4v", "wma", "aiff")
    fun scan(roots: List<Path>): List<Media> {
        val found = linkedMapOf<String, Media>()
        for (root in roots) {
            if (!Files.isDirectory(root)) continue
            val realRoot = runCatching { root.toRealPath() }.getOrNull() ?: continue
            // No FOLLOW_LINKS: never crawl a junction outside an approved root.
            Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(directory: Path, attributes: java.nio.file.attribute.BasicFileAttributes): FileVisitResult =
                    if (runCatching { directory.toRealPath().startsWith(realRoot) }.getOrDefault(false)) FileVisitResult.CONTINUE else FileVisitResult.SKIP_SUBTREE
                override fun visitFile(file: Path, attributes: java.nio.file.attribute.BasicFileAttributes): FileVisitResult {
                    if (attributes.isRegularFile && file.fileName.toString().substringAfterLast('.', "").lowercase(Locale.ROOT) in extensions) {
                        val id = file.toAbsolutePath().normalize().toString()
                        found[id] = Media(id, file.fileName.toString(), attributes.size(), attributes.lastModifiedTime().toMillis())
                    }
                    return FileVisitResult.CONTINUE
                }
                override fun visitFileFailed(file: Path, error: java.io.IOException) = FileVisitResult.CONTINUE
            })
        }
        return found.values.sortedBy { it.name.lowercase(Locale.ROOT) }
    }
}

class DesktopStore(val directory: Path = Path.of(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "GreaterArtDesktop")) {
    private val file = directory.resolve("settings-v1.properties")
    val values = Properties()
    init { if (Files.isRegularFile(file)) runCatching { Files.newInputStream(file).use(values::load) } }
    @Synchronized fun save() {
        Files.createDirectories(directory)
        val temp = directory.resolve("settings-v1.tmp")
        Files.newOutputStream(temp).use { values.store(it, "Greater Art Desktop schema 1") }
        try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
        catch (_: AtomicMoveNotSupportedException) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING) }
    }
    fun roots(): List<Path> = (0 until values.getProperty("roots.count", "0").toIntOrNull().let { (it ?: 0).coerceIn(0, 128) })
        .mapNotNull { values.getProperty("roots.$it")?.let { raw -> runCatching { Path.of(raw) }.getOrNull() } }
    fun setRoots(roots: List<Path>) { values.keys.filter { it.toString().startsWith("roots.") }.toList().forEach(values::remove)
        values["roots.count"] = roots.size.toString(); roots.forEachIndexed { index, path -> values["roots.$index"] = path.toString() }; save() }
    fun favorites(): Set<String> = values.stringPropertyNames().filter { it.startsWith("favorite.") }.mapNotNull { values.getProperty(it) }.toSet()
    fun setFavorites(paths: Set<String>) { values.keys.filter { it.toString().startsWith("favorite.") }.toList().forEach(values::remove)
        paths.sorted().forEachIndexed { index, path -> values["favorite.$index"] = path }; save() }
    fun pane(key: String, fallback: Float): Float = values.getProperty(key)?.toFloatOrNull()?.takeIf { it.isFinite() }?.coerceIn(.18f, .45f) ?: fallback
}
