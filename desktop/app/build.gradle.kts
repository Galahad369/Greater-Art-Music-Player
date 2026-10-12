import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
    id("org.jetbrains.compose") version "1.9.3"
}
kotlin { jvmToolchain(21) }
dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("net.java.dev.jna:jna:5.17.0")
    testImplementation(kotlin("test-junit"))
}
// Compile unchanged pure graph algorithms, without modifying Android's build.
sourceSets.main { kotlin.srcDir("../../greater-art/app/src/main/java/com/local/listentomusic/graph")
    kotlin.exclude("GraphRepository.kt") }
compose.desktop {
    application {
        mainClass = "art.greater.desktop.MainKt"
        nativeDistributions {
            appResourcesRootDir.set(project.layout.projectDirectory.dir("runtime"))
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "GreaterArtDesktop"
            packageVersion = "0.1.0"
            description = "Greater Art Desktop — local audio and video"
            vendor = "Greater Art"
            modules("java.desktop", "java.logging", "java.management", "jdk.unsupported")
            windows { perUserInstall = true; menuGroup = "Greater Art"; shortcut = true }
        }
    }
}
tasks.register<JavaExec>("backendProbe") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("art.greater.desktop.BackendProbeKt")
    args(providers.gradleProperty("fixture").orElse("").get())
}
