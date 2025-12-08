plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    js(IR) {
        browser {
            binaries.executable()
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":sigil-summon"))
                implementation(project(":sigil-schema"))
                implementation(libs.summon)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.server.html.builder)
                implementation(libs.logback.classic)
            }
        }

        jsMain {
            dependencies {
                implementation(libs.materia.engine)
            }
        }
    }
}
