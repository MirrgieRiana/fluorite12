import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("com.dorongold.task-tree") version "4.0.1"
    id("build-logic")
}

kotlin {

    jvmToolchain(21)

    jvm()
    js {
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
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
                implementation("com.squareup.okio:okio:3.10.2")
                implementation("com.ionspin.kotlin:bignum:0.3.10")
                implementation("mirrg.kotlin:mirrg.kotlin.helium-kotlin-2-2:4.0.1")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            }
        }
    }

}

tasks.named("jsBrowserProductionWebpack") { dependsOn("jsProductionLibraryCompileSync") }
tasks.named("jsBrowserProductionLibraryDistribution") { dependsOn("jsProductionExecutableCompileSync") }
tasks.named("jsNodeProductionLibraryDistribution") { dependsOn("jsProductionExecutableCompileSync") }

val releaseExecutable = kotlin.targets
    .withType<KotlinNativeTarget>()
    .getByName("linuxX64")
    .binaries
    .getExecutable("flc", "RELEASE")


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
    from(releaseExecutable.outputFile) { into("bin") }
    from(tasks.named<Jar>("jvmJar")) { into("libs") }
    from("doc") { into("doc") }
    from(project(":playground").layout.buildDirectory.dir("out")) { into("playground") }
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
    dependsOn(generateDocShellTests, releaseExecutable.linkTaskProvider)
    workingDir = project.layout.buildDirectory.file("docShellTests").get().asFile
    commandLine("bash", "ja.sh", releaseExecutable.outputFile.relativeTo(workingDir).invariantSeparatorsPath)
}
tasks.named("check").configure { dependsOn(tasks.named("runDocShellTests")) }


// Utilities

allprojects {
    tasks.register("downloadDependencies") {
        doLast {
            configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
        }
    }
}
