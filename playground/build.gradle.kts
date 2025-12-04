tasks.register<Delete>("clean") {
    delete("build")
}

tasks.register<Exec>("compileEditor") {
    workingDir = projectDir
    commandLine("bash", "compile-editor.sh")
    standardOutput = System.out
    errorOutput = System.err
}

tasks.register<Sync>("build") {
    dependsOn("compileEditor")
    from("src") {
        filesMatching("/index.html") {
            filteringCharset = "UTF-8"
            filter {
                it.replace("<%= APP_VERSION %>", System.getenv("APP_VERSION") ?: "nightly build")
            }
        }
    }
    from(project.layout.buildDirectory.dir("editor"))
    from(rootProject.tasks.named("jsBrowserProductionLibraryDistribution"))
    into(project.layout.buildDirectory.dir("out"))

    // browserとnodejsで異なるタスクが同じディレクトリに出力するKotlin Multiplatformの構造的問題のための苦肉の対策のために書かされている
    mustRunAfter(rootProject.tasks.named("jsNodeProductionLibraryDistribution"))
}
