plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
}

group = "io.github.codeyousef"
version = "0.1.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version

    // Apply common configuration for all subprojects
    afterEvaluate {
        // Enable hierarchical project structure for all KMP modules
        extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()?.apply {
            // Common compiler options
            targets.all {
                compilations.all {
                    compileTaskProvider.configure {
                        compilerOptions {
                            freeCompilerArgs.add("-Xexpect-actual-classes")
                        }
                    }
                }
            }
        }
    }
}

// Maven Central Publishing Configuration
allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}
