// health-calculations: the health formulas OpenVitals and OpenWatch share. Plain
// Kotlin on the JDK, so the phone apps, OpenWatch's :wearlink and the Wear OS app
// all take the same code. Each app takes it as a composite build (includeBuild)
// from a git submodule, as it takes OpenLink.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "health-calculations"
