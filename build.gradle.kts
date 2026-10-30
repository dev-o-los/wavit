plugins {
    kotlin("multiplatform") version "2.1.0" apply false
    kotlin("jvm") version "2.1.0" apply false
    kotlin("android") version "2.1.0" apply false
    kotlin("plugin.serialization") version "2.1.0" apply false
    kotlin("plugin.compose") version "2.1.0" apply false
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    id("io.ktor.plugin") version "3.0.3" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}


