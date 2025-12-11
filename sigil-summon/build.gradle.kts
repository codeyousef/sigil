plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()
    js(IR) {
        browser {
            // Create executable JS bundle for hydration
            binaries.executable()
            
            webpackTask {
                mainOutputFileName = "sigil-hydration.js"
            }
            
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs {
            testTask {
                useMocha()
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":sigil-schema"))
                implementation(libs.summon)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        jvmMain {
            dependencies {
                // Framework integrations (compileOnly - users provide their own)
                compileOnly(libs.ktor.server.core)      // Ktor
                compileOnly(libs.jakarta.servlet.api)   // Spring Boot / Quarkus
            }
        }

        jsMain {
            dependencies {
                implementation(libs.materia.engine)
            }
        }
    }
}

// Copy compiled JS bundle to JVM resources for sigilStaticAssets()
val copyJsBundleToJvmResources by tasks.registering(Copy::class) {
    dependsOn("jsBrowserProductionWebpack")
    from(layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable"))
    into(layout.projectDirectory.dir("src/jvmMain/resources/static"))
    include("sigil-hydration.js", "sigil-hydration.js.map")
}

// Ensure the JS bundle is copied before JVM JAR is built
tasks.named("jvmProcessResources") {
    dependsOn(copyJsBundleToJvmResources)
}

// Also ensure it runs before publishing
tasks.matching { it.name.contains("publish", ignoreCase = true) }.configureEach {
    dependsOn(copyJsBundleToJvmResources)
}
