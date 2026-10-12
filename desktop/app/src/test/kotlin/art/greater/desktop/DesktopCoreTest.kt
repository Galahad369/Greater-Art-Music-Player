package art.greater.desktop

import com.local.listentomusic.graph.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.*
import java.nio.file.Files

class DesktopCoreTest {
    @Test fun scansOnlyChosenRootsPreservesCjkAndDistinctSameNames() {
        val root = Files.createTempDirectory("ga-desktop-test")
        try {
            val chosen = Files.createDirectory(root.resolve("chosen"))
            Files.write(chosen.resolve("孤独毒毒.mp3"), byteArrayOf(1))
            Files.createDirectory(chosen.resolve("nested")); Files.write(chosen.resolve("nested/孤独毒毒.mp3"), byteArrayOf(2))
            Files.write(root.resolve("outside.mp3"), byteArrayOf(3))
            Files.write(chosen.resolve("not-media.txt"), byteArrayOf(3))
            val media = LocalLibrary.scan(listOf(chosen, chosen))
            assertEquals(2, media.size); assertEquals(2, media.map { it.id }.toSet().size)
            assertTrue(media.all { it.name == "孤独毒毒.mp3" })
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun settingsRootsFavoritesAndPaneSizesRoundTripAtomically() {
        val root = Files.createTempDirectory("ga-store-test")
        try {
            val store = DesktopStore(root)
            store.setRoots(listOf(root.resolve("歌")))
            store.setFavorites(setOf(root.resolve("歌/Caravan.mp3").toString()))
            store.values["pane.left"] = "NaN"; store.save()
            val restored = DesktopStore(root)
            assertEquals(store.roots(), restored.roots()); assertEquals(store.favorites(), restored.favorites())
            assertEquals(.28f, restored.pane("pane.left", .28f))
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun usesActualAndroidSimilarityAndDeterministicLayout() = runBlocking {
        assertTrue(FilenameSimilarity.score("Caravan.mp3", "Gambling.mp4") > 0)
        assertEquals(1.0, FilenameSimilarity.score("孤独毒毒.mp4", "孤独毒毒.mp3"))
        val nodes = listOf(GraphInput("1", "孤独毒毒.mp3"), GraphInput("2", "孤独毒毒 cover.mp4"), GraphInput("3", "other.wav"))
        val edges = GraphBuilder.edges(nodes)
        assertTrue(edges.isNotEmpty())
        assertEquals(GraphLayout.settle(nodes, edges), GraphLayout.settle(nodes, edges))
    }
}
