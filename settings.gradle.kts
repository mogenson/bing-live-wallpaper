pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://clojars.org/repo") }
    }
}

rootProject.name = "bing-live-wallpaper"
include(":app")
