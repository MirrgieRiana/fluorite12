import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "1.6.20"
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
        }
        nodejs()
    }
    linuxX64 {
        binaries {
            executable("flc")
        }
    }
    mingwX64 {
        binaries {
            executable("flc")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.github.h0tk3y.betterParse:better-parse:0.4.4")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val jvmTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
        kotlin.targets.withType<KotlinNativeTarget>().all {
            compilations.getByName("main") {
                defaultSourceSet.dependsOn(nativeMain)
            }
        }
    }

}
