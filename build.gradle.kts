plugins {
    application
    kotlin("jvm") version "1.7.10"
}

group = "me.tobias"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "2.1.1"

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.hid4java:hid4java:0.7.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("org.json:json:20220320")
    implementation("com.google.code.gson:gson:2.9.1")
    implementation("com.adamratzman:spotify-api-kotlin-core:3.8.8")
    implementation("io.javalin:javalin:4.6.4")
}


tasks.test {
    useJUnit()
}

application {
    mainClass.set("MainKt")
}
