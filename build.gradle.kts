
plugins {
    kotlin("multiplatform") version "1.6.20"
    id("com.dorongold.task-tree") version "2.1.1"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
}
