pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // SQLCipher متاح على Maven Central عبر sqlcipher-android
    }
}

rootProject.name = "CryptoSafe"
include(":app")
