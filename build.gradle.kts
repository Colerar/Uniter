import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.30")
        classpath("com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:0.11.0")
    }
}

plugins {
    application
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
    id("dev.petuska.npm.publish")
    id("com.codingfeline.buildkonfig") version "0.11.0"
}

group = "cli"
version = "1.0.0-DEV"

@Suppress("PropertyName")
val PROGRAM = "uniter"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(Testing.junit.jupiter.params)
    testRuntimeOnly(Testing.junit.jupiter.engine)
}

application {
    mainClass.set("cli.JvmMainKt")
}

val hostOs: String = System.getProperty("os.name")
val nativeTarget = when {
    hostOs == "Mac OS X" -> "MacosX64"
    hostOs == "Linux" -> "LinuxX64"
    hostOs.startsWith("Windows") -> "MingwX64"
    else -> throw GradleException("Host $hostOs is not supported in Kotlin/Native.")
}

fun KotlinNativeTarget.configureTarget() =
    binaries { executable { entryPoint = "main" } }

kotlin {

    macosX64 { configureTarget() }
    mingwX64 { configureTarget() }
    linuxX64 { configureTarget() }

    val jvmTarget = jvm()

    /**
     *   common
     *   |-- jvm
     *   '-- native
     *      |- posix
     *      |   |-- macosX64
     *      |   |-- linuxX64
     *      '-- mingw
     *         '-- mingwX64
     */
    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }

        val commonMain by getting {
            dependencies {
                // Clikt - https://ajalt.github.io/clikt/
                implementation("com.github.ajalt.clikt:clikt:_")
                implementation("com.github.ajalt.mordant:mordant:_")
                // OKIO - https://square.github.io/okio/
                implementation("com.squareup.okio:okio-multiplatform:_")
                // Coroutines - https://github.com/Kotlin/kotlinx.coroutines/
                implementation(KotlinX.coroutines.core)
                // Serialization - https://github.com/Kotlin/kotlinx.serialization
                implementation(KotlinX.serialization.core)
                implementation(KotlinX.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(Kotlin.test.common)
                implementation(Kotlin.test.annotationsCommon)
            }
        }
        getByName("jvmMain") {
            dependsOn(commonMain)
            dependencies {
                // implementation(Ktor.client.okHttp)
                // implementation(Square.okHttp3.okHttp)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(Testing.junit.jupiter.api)
                implementation(Testing.junit.jupiter.engine)
                implementation(Kotlin.test.junit5)
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                /// implementation(Ktor.client.curl)
            }
        }
        val nativeTest by creating {
            dependsOn(commonTest)
        }
        val posixMain by creating {
            dependsOn(nativeMain)
        }
        val posixTest by creating {
            dependsOn(nativeTest)
        }
        arrayOf("macosX64", "linuxX64").forEach { targetName ->
            getByName("${targetName}Main").dependsOn(posixMain)
            getByName("${targetName}Test").dependsOn(posixTest)
        }
        arrayOf("macosX64", "linuxX64", "mingwX64").forEach { targetName ->
            getByName("${targetName}Main").dependsOn(nativeMain)
            getByName("${targetName}Test").dependsOn(nativeTest)
        }

        sourceSets {
            all {
                languageSettings.optIn("kotlin.RequiresOptIn")
                languageSettings.optIn("okio.ExperimentalFileSystem")
            }
        }
    }

    tasks.withType<JavaExec> {
        // code to make run task in kotlin multiplatform work
        val compilation = jvmTarget.compilations.getByName<KotlinJvmCompilation>("main")

        val classes = files(
            compilation.runtimeDependencyFiles,
            compilation.output.allOutputs
        )
        classpath(classes)
    }
    tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
        archiveBaseName.set(project.name)
        archiveClassifier.set("")
        archiveVersion.set("")

        from(jvmTarget.compilations.getByName("main").output)
        configurations = mutableListOf(
            jvmTarget.compilations.getByName("main").compileDependencyFiles,
            jvmTarget.compilations.getByName("main").runtimeDependencyFiles
        )
    }
}

val commitHash by lazy {
    val commitHashCommand = "git rev-parse --short HEAD"
    Runtime.getRuntime().exec(commitHashCommand).inputStream.bufferedReader().readLine() ?: "UnknownCommit"
}

val branch by lazy {
    val branchCommand = "git rev-parse --abbrev-ref HEAD"
    Runtime.getRuntime().exec(branchCommand).inputStream.bufferedReader().readLine() ?: "UnknownBranch"
}

val time: String by lazy {
    ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
}

buildkonfig {
    packageName = "cli"
    defaultConfigs {
        buildConfigField(STRING, "version", version.toString())
        buildConfigField(STRING, "versionLong", "$version-[$branch]$commitHash $time")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<Copy>("install") {
    group = "run"
    description = "Build the native executable and install it"
    val destDir = "/usr/local/bin"

    dependsOn("linkDebugExecutable$nativeTarget")
    val targetLowercase = nativeTarget.first().toLowerCase() + nativeTarget.substring(1)
    val folder = "build/bin/$targetLowercase/debugExecutable"
    from(folder) {
        include("${rootProject.name}.kexe")
        rename { PROGRAM }
    }
    into(destDir)
    doLast {
        println("$ cp $folder/${rootProject.name}.kexe $destDir/$PROGRAM")
    }
}

tasks.register("allRun") {
    group = "run"
    description = "Run $PROGRAM on the JVM and natively"
    dependsOn("run", "runDebugExecutable$nativeTarget")
}

tasks.register("buildReleases") {
    group = "build"
    description = "Run $PROGRAM on the JVM and natively"
    doFirst {
        delete("./build/native")
    }
    dependsOn("linkReleaseExecutableLinuxX64", "linkReleaseExecutableMacosX64", "linkReleaseExecutableMingwX64")
    doLast {
        runBlocking {
            val projectName = rootProject.name
            File("./build/bin").listFiles().orEmpty().asFlow()
                .filterNotNull()
                .filter { it.isDirectory }
                .onEach { dir ->
                    val file = File(dir, "releaseExecutable").listFiles().orEmpty().filterNotNull().first {
                        it.name.startsWith(projectName) && (it.extension == "exe" || it.extension == "kexe")
                    }
                    copy {
                        from(file.parent)
                        val name = file.name
                        include(name)
                        rename {
                            if (it.endsWith(".exe")) it else rootProject.name
                        }
                        into("build/native/${file.parentFile.parentFile.name}-$version")
                    }
                }
                .collect()
        }
    }
}

interface Injected {
    @get:Inject
    val exec: ExecOperations

    @get:Inject
    val fs: FileSystemOperations
}

tasks.register("completions") {
    group = "run"
    description = "Generate Bash/Zsh/Fish completion files"
    dependsOn(":install")
    val injected = project.objects.newInstance<Injected>()
    val shells = listOf(
        Triple("bash", file("completions/uniter.bash"), "/usr/local/etc/bash_completion.d"),
        Triple("zsh", file("completions/_uniter.zsh"), "/usr/local/share/zsh/site-functions"),
        Triple("fish", file("completions/uniter.fish"), "/usr/local/share/fish/vendor_completions.d"),
    )
    for ((SHELL, FILE, INSTALL) in shells) {
        actions.add {
            println("Updating   $SHELL completion file at $FILE")
            injected.exec.exec {
                commandLine("uniter", "--generate-completion", SHELL)
                standardOutput = FILE.outputStream()
            }
            println("Installing $SHELL completion into $INSTALL")
            injected.fs.copy {
                from(FILE)
                into(INSTALL)
            }
        }
    }
    doLast {
        println("On macOS, follow those instructions to configure shell completions")
        println("👀 https://docs.brew.sh/Shell-Completion 👀")
    }
}
