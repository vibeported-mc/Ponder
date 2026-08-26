plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.23"
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("ponderPlugin") {
            id = "net.createmod.ponder.gradle"
            implementationClass = "net.createmod.pondergradle.PonderGradlePlugin"
        }
    }
}
