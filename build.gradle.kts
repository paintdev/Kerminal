plugins {
    kotlin("jvm") version "2.4.20"
    id("com.gradleup.shadow") version "9.6.1"
    application
}

group = "org.paintdev"
version = "1.3-2-test"

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

dependencies {
    implementation("org.jetbrains.jediterm:jediterm-core:3.76")
    implementation("org.jetbrains.jediterm:jediterm-ui:3.76")
    implementation("org.jetbrains.pty4j:pty4j:0.13.10")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.9")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass = "org.paintdev.MainKt"
}

tasks.test {
    useJUnitPlatform()
}