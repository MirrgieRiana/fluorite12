plugins {
    kotlin("js")
}

repositories {
    mavenCentral()
}

kotlin {
    js {
        browser {
            binaries.executable()
        }
        sourceSets["main"].dependencies {
            implementation(project(":"))
        }
    }
}
