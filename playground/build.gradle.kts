tasks.register<Delete>("clean") {
    delete("build")
}

tasks.register<Exec>("compileEditor") {
    workingDir = projectDir
    commandLine("bash", "compile-editor.sh")
    standardOutput = System.out
    errorOutput = System.err
}

tasks.register<Copy>("build") {
    dependsOn("compileEditor")
    dependsOn(":jsBrowserProductionWebpack")
    from("src") {
        filesMatching("/index.html") {
            filteringCharset = "UTF-8"
            filter {
                it.replace("<%= APP_VERSION %>", System.getenv("APP_VERSION") ?: "nightly build")
            }
        }
    }
    from("build/editor")
    from(rootProject.layout.buildDirectory.dir("distributions"))
    into("build/out")
}
