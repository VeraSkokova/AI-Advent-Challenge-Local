plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("application")
}

group = "ru.skokova.aiadventchallenge"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:3.0.0")
    implementation("io.ktor:ktor-client-cio:3.0.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")
    implementation("io.ktor:ktor-client-plugins:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("de.kherud:java-llama.cpp:2.3.4")
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(17)
}
