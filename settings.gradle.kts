pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "ponder"

// NeoForge only: the Fabric modules are left in the tree but out of the build.
for (platform in listOf("common", "neoforge")) {
    include(platform)

    include(":catnip:$platform")
    include(":testmod:$platform")
}

includeBuild("build-logic")
