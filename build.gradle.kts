import java.util.Properties
import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(localFile.inputStream())
    }
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        load(versionPropsFile.inputStream())
    }
}

group = "io.github.codeyousef"
version = versionProps.getProperty("VERSION") ?: "0.1.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "maven-publish")

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

// Custom task to bundle and publish artifacts to Maven Central Portal
tasks.register("publishToCentralPortalManually") {
    group = "publishing"
    description = "Publish to Maven Central using Central Portal API"
    dependsOn(subprojects.map { ":${it.name}:publishToMavenLocal" })

    doLast {
        val username = localProperties.getProperty("mavenCentralUsername")
            ?: System.getenv("mavenCentralUsername")
            ?: throw GradleException("mavenCentralUsername not found")

        val password = localProperties.getProperty("mavenCentralPassword")
            ?: System.getenv("mavenCentralPassword")
            ?: throw GradleException("mavenCentralPassword not found")

        println("🚀 Publishing to Maven Central via Central Portal API...")
        println("📦 Username: $username")

        // Create bundle directory with proper Maven structure
        val bundleDir = file("${layout.buildDirectory.get()}/central-portal-bundle")
        bundleDir.deleteRecursively()

        // List of modules to publish
        val modules = listOf("sigil-schema", "sigil-compose", "sigil-summon")
        // List of variants for each module
        val variants = listOf("", "-jvm", "-js") // Empty string for metadata artifact

        val allFilesToProcess = mutableListOf<File>()

        modules.forEach { module ->
            variants.forEach { variant ->
                val artifactId = "$module$variant"
                val version = project.version.toString()
                
                // Construct path in local repo
                val localMavenDir = file("${System.getProperty("user.home")}/.m2/repository/io/github/codeyousef/$artifactId/$version")
                
                if (localMavenDir.exists()) {
                    val mavenPath = "io/github/codeyousef/$artifactId/$version"
                    val targetDir = file("$bundleDir/$mavenPath")
                    targetDir.mkdirs()

                    println("📦 Processing $artifactId artifacts...")

                    localMavenDir.listFiles()?.forEach { file ->
                        if ((file.name.endsWith(".jar") || file.name.endsWith(".pom") || file.name.endsWith(".klib") || file.name.endsWith(".module")) &&
                            !file.name.endsWith(".md5") && !file.name.endsWith(".sha1") && !file.name.endsWith(".asc")) {
                            
                            file.copyTo(File(targetDir, file.name), overwrite = true)
                            allFilesToProcess.add(File(targetDir, file.name))
                        }
                    }
                } else {
                    println("⚠️ No artifacts found for $artifactId at $localMavenDir")
                }
            }
        }

        if (allFilesToProcess.isEmpty()) {
            throw GradleException("No Maven artifacts found. Make sure publishToMavenLocal ran successfully.")
        }

        println("📝 Generating checksums and signatures...")

        allFilesToProcess.forEach { file ->
            // Generate MD5 checksum
            val md5Hash = MessageDigest.getInstance("MD5")
                .digest(file.readBytes())
                .joinToString("") { byte -> "%02x".format(byte) }
            File(file.parent, "${file.name}.md5").writeText(md5Hash)

            // Generate SHA1 checksum
            val sha1Hash = MessageDigest.getInstance("SHA-1")
                .digest(file.readBytes())
                .joinToString("") { byte -> "%02x".format(byte) }
            File(file.parent, "${file.name}.sha1").writeText(sha1Hash)

            // Generate GPG signature
            val sigFile = File(file.parent, "${file.name}.asc")
            println("   Creating GPG signature for ${file.name}...")

            val privateKeyFile = rootProject.file("private-key.asc")
            if (!privateKeyFile.exists()) {
                throw GradleException("private-key.asc not found. Cannot sign artifacts.")
            }

            val signScript = rootProject.file("sign-artifact.sh")
            if (!signScript.exists()) {
                throw GradleException("sign-artifact.sh not found. Cannot sign artifacts.")
            }

            val signingPassword = localProperties.getProperty("signingPassword")
                 ?: System.getenv("signingPassword")
                 ?: throw GradleException("signingPassword not found")

            exec {
                commandLine(
                    "bash",
                    signScript.absolutePath,
                    signingPassword,
                    privateKeyFile.absolutePath,
                    sigFile.absolutePath,
                    file.absolutePath
                )
            }
        }

        println("📦 Zipping bundle...")
        val zipFile = file("${layout.buildDirectory.get()}/central-portal-bundle.zip")
        if (zipFile.exists()) zipFile.delete()

        exec {
            workingDir = bundleDir
            commandLine("zip", "-r", zipFile.absolutePath, ".")
        }

        println("🚀 Uploading to Central Portal...")
        // Base64 encode credentials
        val userPass = "$username:$password"
        val userPassBase64 = java.util.Base64.getEncoder().encodeToString(userPass.toByteArray())

        // Upload using curl
        // Note: Using 'publishingType=AUTOMATIC' to automatically release if validation passes
        exec {
            commandLine(
                "curl",
                "--request", "POST",
                "--url", "https://central.sonatype.com/api/v1/publisher/upload?publishingType=AUTOMATIC",
                "--header", "Authorization: Bearer $userPassBase64",
                "--form", "bundle=@${zipFile.absolutePath}"
            )
        }
        
        println("✅ Upload complete!")
    }
}
