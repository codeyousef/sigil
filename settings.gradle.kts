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
        // Node.js distribution for Kotlin/JS tests
        ivy("https://nodejs.org/dist/") {
            name = "nodejs-dist"
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("org.nodejs", "node")
            }
        }
        // Yarn distribution for Kotlin/JS package management
        ivy("https://github.com/yarnpkg/yarn/releases/download/") {
            name = "yarn-dist"
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("com.yarnpkg", "yarn")
            }
        }
    }
}

rootProject.name = "sigil"

include(":sigil-schema")
include(":sigil-compose")
include(":sigil-summon")
include(":sample-cmp")
include(":sample-summon")
