pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "sigil"

include(":sigil-schema")
include(":sigil-compose")
include(":sigil-summon")
include(":sample-cmp")
include(":sample-summon")
