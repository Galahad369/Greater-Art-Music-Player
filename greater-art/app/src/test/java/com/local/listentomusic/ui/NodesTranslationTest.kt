package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage
import org.junit.Assert.*
import org.junit.Test

class NodesTranslationTest {
    @Test fun graphControlsAndFeedbackNeverFallBackToEnglish() {
        val keys = listOf("Retry", "Preparing filename connections…",
            "Your library is empty. Add media in Download, then refresh Library.",
            "Fit", "Find", "Playing", "Controls", "nodes", "links", "Find a node", "Filename", "Close",
            "Graph controls", "Connections", "Minimum similarity", "Link visibility", "Hide isolated nodes",
            "Nodes", "Node size", "Filename labels", "Size by strong connections", "Restore graph defaults",
            "Apply", "Obsidian-style display and forces", "No system equalizer is installed on this device.",
            "The double-ring node is playing. Larger nodes have more strong filename connections.",
            "Tap node to play  •  Drag node  •  Pinch to zoom  •  Swipe empty space to navigate",
            "Filename similarity graph. Pinch to zoom, drag individual nodes, tap a node to play, or swipe empty space to change page. Use Find / play a node for a text list.")
        AppLanguage.entries.filter { it != AppLanguage.ENGLISH }.forEach { language ->
            keys.forEach { key -> assertNotEquals("$language: $key", key, uiText(language, key, key)) }
        }
    }
}
