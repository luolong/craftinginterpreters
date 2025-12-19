// For more detailed information on multi-project builds, please refer to https://docs.gradle.org/9.2.1/userguide/multi_project_builds.html in the Gradle documentation.

pluginManagement {
    // Include 'plugins build' to define convention plugins.
    includeBuild("build-logic")
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "craftinginterpreters"
include("lox")
