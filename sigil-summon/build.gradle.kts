plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()
    js(IR) {
        browser {
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
                // JVM-specific Summon dependencies for SSR
                // Ktor integration dependencies
                compileOnly(libs.ktor.server.core)
            }
        }

        jsMain {
            dependencies {
                implementation(libs.materia.engine)
            }
        }
    }
}

// TODO: In the future, add task to copy compiled JS into JVM resources
// For now, we use a static placeholder in src/jvmMain/resources/static/sigil-hydration.js
