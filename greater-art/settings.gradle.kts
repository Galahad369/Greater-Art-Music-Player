pluginManagement {
    repositories {
        google { content { excludeGroup("org.jetbrains.kotlin") } }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Kotlin compiler/runtime tooling is published on Maven Central. Avoid
        // probing Google first for release-only Compose mapping dependencies.
        google { content { excludeGroup("org.jetbrains.kotlin") } }
        mavenCentral()
    }
}

rootProject.name = "GreaterArt"
include(":app")
