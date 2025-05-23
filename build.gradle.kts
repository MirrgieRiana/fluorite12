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

tasks.register<Exec>("compilePlayground") {
    workingDir = file("playground")
    commandLine("bash", "compile.sh")
    standardOutput = System.out
    errorOutput = System.err
    dependsOn("jsBrowserProductionWebpack")
}

tasks.named("assemble") {
    dependsOn("compilePlayground")
}

allprojects {
    tasks.register("downloadDependencies") {
        doLast {
            configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
        }
    }
}

class DocShellParser(private val filePath: String, private val lines: List<String>, private val out: (String) -> Unit) {

    private inner class Pointer {
        var index = 0
        val lineNumber get() = index + 1

        fun get() = lines.getOrNull(index)

        fun next() {
            index++
        }

        fun throwParseError(message: String): Nothing = throw IllegalStateException("$filePath($lineNumber): $message")
    }

    class DocShell(val lineNumber: Int, val command: List<String>, val expected: List<String>)

    fun writeTestScriptHeader() {
        out("#!/usr/bin/env bash")
        out("fail() {")
        out("  echo \"FAILED: \$1\" >&2")
        out("  echo \"[expected]\" >&2")
        out("  echo \"\$2\" >&2")
        out("  echo \"[actual]\" >&2")
        out("  echo \"\$3\" >&2")
        out("  exit 1")
        out("}")
    }

    fun writeTestScript() {
        parseDoc(Pointer()).forEach { docShell ->
            out("")

            out("# $filePath (${docShell.lineNumber})")
            out("expected=$(")
            out("cat << 'DOC_SHELL_TEST_END'")
            docShell.expected.forEach {
                out(it)
            }
            out("DOC_SHELL_TEST_END")
            out(")")
            out("actual=$(")
            docShell.command.forEach {
                out(it)
            }
            out(")")
            out("[ \"\$actual\" = \"\$expected\" ] || fail \"$filePath:${docShell.lineNumber}\" \"\$expected\" \"\$actual\"")
            out("echo \"OK $filePath:${docShell.lineNumber}\"")
        }
    }

    private fun parseDoc(pointer: Pointer): List<DocShell> {
        val docShells = mutableListOf<DocShell>()
        while (true) {
            if (pointer.get() == null) return docShells
            val docShells2 = tryParseDocShell(pointer)
            if (docShells2 != null) {
                docShells += docShells2
            } else {
                pointer.next()
            }
        }
    }

    private fun tryParseDocShell(pointer: Pointer): List<DocShell>? {
        if (pointer.get() == "```shell") {
            pointer.next()

            val docShells = mutableListOf<DocShell>()
            while (true) {
                val lineNumber = pointer.lineNumber
                val command = mutableListOf<String>()
                val expected = mutableListOf<String>()
                command += parseHeadCommandLine(pointer)
                command += parseBodyCommandLine(pointer)
                expected += parseExpectedLine(pointer)
                docShells += DocShell(lineNumber, command, expected)

                if (pointer.get() == "```") {
                    pointer.next()
                    return docShells
                } else if (pointer.get() == "") {
                    pointer.next()
                    continue
                } else {
                    pointer.throwParseError("Expected EndOfDocShell or EmptyLine")
                }
            }
        } else {
            return null
        }
    }

    private fun parseHeadCommandLine(pointer: Pointer): String {
        val line = pointer.get()
        if (line != null && line.startsWith("\$ ")) {
            pointer.next()
            return line.drop(2)
        } else {
            pointer.throwParseError("Expected HeadCommandLine")
        }
    }

    private fun parseBodyCommandLine(pointer: Pointer): List<String> {
        val lines = mutableListOf<String>()
        while (true) {
            val line = pointer.get()
            if (line != null && !line.startsWith("# ") && line != "#" && line != "```") {
                lines += line
                pointer.next()
            } else {
                break
            }
        }
        return lines
    }

    private fun parseExpectedLine(pointer: Pointer): List<String> {
        val lines = mutableListOf<String>()
        while (true) {
            val line = pointer.get()
            if (line != null && line.startsWith("# ")) {
                lines += line.drop(2)
                pointer.next()
            } else if (line != null && line == "#") {
                lines += ""
                pointer.next()
            } else {
                break
            }
        }
        return lines
    }

}

tasks.register("generateDocShellTests") {
    val docsDir = file("doc/ja")
    val outFile = file("build/docShellTests/ja.sh")

    inputs.dir(docsDir)
    outputs.file(outFile)

    doLast {
        outFile.parentFile.mkdirs()
        val lines = mutableListOf<String>()
        docsDir.walkTopDown().filter { it.extension == "md" }.forEachIndexed { index, file ->
            val docShellParser = DocShellParser(file.toRelativeString(projectDir).replace('\\', '/'), file.readLines(), lines::add)
            if (index == 0) docShellParser.writeTestScriptHeader()
            docShellParser.writeTestScript()
        }
        outFile.writeText(lines.joinToString("") { "$it\n" })
        outFile.setExecutable(true)
    }
}
