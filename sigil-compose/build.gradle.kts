plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":sigil-schema"))
                implementation(libs.materia.engine)
                implementation(libs.compose.runtime)
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
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
