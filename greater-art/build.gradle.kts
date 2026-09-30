plugins {
    // Verified together for the offline build. Do not accept dependency-bot versions
    // until Google/Maven resolution and the full device-independent gate both pass.
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}

