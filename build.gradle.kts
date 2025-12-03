import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

plugins {
    kotlin("multiplatform") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("com.dorongold.task-tree") version "4.0.1"
    id("build-logic")
}

repositories {
    mavenCentral()
    maven("https://raw.githubusercontent.com/MirrgieRiana/mirrg.kotlin/refs/heads/maven/maven/")
}

kotlin {

    jvmToolchain(21)

    jvm()
    js(IR) {
        outputModuleName = "fluorite12"
        browser {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
        binaries.executable()
        binaries.library()
    }
    linuxX64 {
        binaries {
            executable("flc")
        }
    }
    // mingwX64だけ同じテストが成功したり失敗したりする怪現象のため廃止
    //mingwX64 {
    //    binaries {
    //        executable("flc")
    //    }
    //}

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
                implementation("com.squareup.okio:okio:3.10.2")
                implementation("com.ionspin.kotlin:bignum:0.3.10")
                implementation("mirrg.kotlin:mirrg.kotlin.helium-kotlin-2-2:4.0.1")
                compileOnly(kotlin("test")) // ここにも書かないとなぜかIDEAが認識しない
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        kotlin.targets.withType<KotlinNativeTarget>().all {
            compilations.getByName("main") {
                defaultSourceSet.dependsOn(nativeMain)
            }
        }
    }

}

tasks.named("jsBrowserProductionWebpack") { dependsOn("jsProductionLibraryCompileSync") }
tasks.named("jsBrowserProductionLibraryDistribution") { dependsOn("jsProductionExecutableCompileSync") }
tasks.named("jsNodeProductionLibraryDistribution") { dependsOn("jsProductionExecutableCompileSync") }


// Executable Jar

tasks.named<Jar>("jvmJar") {
    manifest {
        attributes["Main-Class"] = "JvmMainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations["jvmRuntimeClasspath"].filter { it.name.endsWith(".jar") }.map { zipTree(it) }
    })
}


// Release

tasks.register<Copy>("generateInstallNative") {
    from(file("release-template/install.sh"))
    filteringCharset = "UTF-8"
    filter {
        it
            .replace("@ENGINE@", "native")
            .replace("@SCRIPT_NAME@", "install-native.sh")
    }
    filePermissions {
        unix("rwxr-xr-x")
    }
    rename("install.sh", "install-native.sh")
    into("build/generateInstallNative")
}
tasks.register<Copy>("generateInstallJvm") {
    from(file("release-template/install.sh"))
    filteringCharset = "UTF-8"
    filter {
        it
            .replace("@ENGINE@", "jvm")
            .replace("@SCRIPT_NAME@", "install-jvm.sh")
    }
    filePermissions {
        unix("rwxr-xr-x")
    }
    rename("install.sh", "install-jvm.sh")
    into("build/generateInstallJvm")
}


tasks.register<Sync>("bundleRelease") {
    dependsOn("build", ":playground:build", "generateInstallNative", "generateInstallJvm")
    into(layout.buildDirectory.dir("bundleRelease"))
    from("release") {
        rename("gitignore", ".gitignore")
    }
    from("build/generateInstallNative")
    from("build/generateInstallJvm")
    from("build/bin") { into("bin") }
    from("build/libs") { into("libs") }
    from("doc") { into("doc") }
    from("playground/build/out") { into("playground") }
}


// Doc Shell Tests

val generateDocShellTests = tasks.register("generateDocShellTests") {
    val docsDir = file("doc/ja")
    val outFile = project.layout.buildDirectory.file("docShellTests/ja.sh")

    inputs.dir(docsDir)
    outputs.file(outFile)

    doLast {
        outFile.get().asFile.parentFile.mkdirs()
        val lines = mutableListOf<String>()
        val docFiles = docsDir.walkTopDown().filter { it.extension == "md" }.toList()
        docFiles.forEachIndexed { index, file ->
            val docShellParser = DocShellParser(file.toRelativeString(projectDir).replace('\\', '/'), file.readLines(), lines::add)
            if (index == 0) docShellParser.writeTestScriptHeader()
            docShellParser.writeTestScript()
            if (index == docFiles.size - 1) docShellParser.writeTestScriptFooter()
        }
        outFile.get().asFile.writeText(lines.joinToString("") { "$it\n" })
        outFile.get().asFile.setExecutable(true)
    }
}

tasks.register<Exec>("runDocShellTests") {
    val linkTask = tasks.named<KotlinNativeLink>("linkFlcReleaseExecutableLinuxX64")
    dependsOn(generateDocShellTests, linkTask)
    workingDir = project.layout.buildDirectory.file("docShellTests").get().asFile
    commandLine("bash", "ja.sh", linkTask.get().outputFile.get().relativeTo(workingDir).invariantSeparatorsPath)
}
tasks.named("check").get().dependsOn(tasks.named("runDocShellTests"))


// Utilities

allprojects {
    tasks.register("downloadDependencies") {
        doLast {
            configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
        }
    }
}
