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
            }
        }

        jsMain {
            dependencies {
                implementation(libs.materia.engine)
            }
        }
    }
}
