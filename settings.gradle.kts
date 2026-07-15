// Settings for the whole build. Names the project and where to fetch plugins/libraries from.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // Fail fast if a module tries to declare its own repositories -> keep everything central.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Elendheim Picture Editor"
include(":app")
