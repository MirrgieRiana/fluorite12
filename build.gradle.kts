import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
    id("com.dorongold.task-tree") version "2.1.1"
}

repositories {
    mavenCentral()
}

kotlin {

    jvm()
    js {
        browser {
            dceTask {
                keep("fluorite12.parse", "fluorite12.evaluate", "fluorite12.log", "fluorite12.stringify")
            }
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
                implementation("com.github.h0tk3y.betterParse:better-parse:0.4.4")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
                implementation("com.ionspin.kotlin:bignum:0.3.6")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
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


// Playground

tasks.register<Delete>("cleanPlayground") {
    delete("playground/build")
}

tasks.register<Exec>("compilePlaygroundEditor") {
    workingDir = file("playground")
    commandLine("bash", "compile-editor.sh")
    standardOutput = System.out
    errorOutput = System.err
}

tasks.register<Copy>("compilePlayground") {
    mustRunAfter("cleanPlayground")
    dependsOn("cleanPlayground")
    dependsOn("compilePlaygroundEditor")
    dependsOn("jsBrowserProductionWebpack")
    from("playground/src")
    from("playground/build/editor")
    from(layout.buildDirectory.dir("distributions"))
    into("playground/build/out")
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
    fileMode = 0b111101101
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
    fileMode = 0b111101101
    rename("install.sh", "install-jvm.sh")
    into("build/generateInstallJvm")
}


tasks.register<Sync>("bundleRelease") {
    dependsOn("build", "compilePlayground", "generateInstallNative", "generateInstallJvm")
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
    println(this.excludes)
}


// Doc Shell Tests

tasks.register("generateDocShellTests") {
    val docsDir = file("doc/ja")
    val outFile = file("build/docShellTests/ja.sh")

    inputs.dir(docsDir)
    outputs.file(outFile)

    doLast {
        outFile.parentFile.mkdirs()
        val lines = mutableListOf<String>()
        val docFiles = docsDir.walkTopDown().filter { it.extension == "md" }.toList()
        docFiles.forEachIndexed { index, file ->
            val docShellParser = DocShellParser(file.toRelativeString(projectDir).replace('\\', '/'), file.readLines(), lines::add)
            if (index == 0) docShellParser.writeTestScriptHeader()
            docShellParser.writeTestScript()
            if (index == docFiles.size - 1) docShellParser.writeTestScriptFooter()
        }
        outFile.writeText(lines.joinToString("") { "$it\n" })
        outFile.setExecutable(true)
    }
}

tasks.register<Exec>("runDocShellTests") {
    dependsOn("generateDocShellTests", "linkFlcReleaseExecutableLinuxX64")
    commandLine("bash", "build/docShellTests/ja.sh", "build/bin/linuxX64/flcReleaseExecutable/flc.kexe")
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
