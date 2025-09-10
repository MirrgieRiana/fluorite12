import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec

tasks.register<Delete>("cleanPlayground") {
    delete("build")
}

tasks.register<Exec>("compilePlaygroundEditor") {
    workingDir = projectDir
    commandLine("bash", "compile-editor.sh")
    standardOutput = System.out
    errorOutput = System.err
}

tasks.register<Copy>("compilePlayground") {
    mustRunAfter("cleanPlayground")
    dependsOn("cleanPlayground")
    dependsOn("compilePlaygroundEditor")
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
