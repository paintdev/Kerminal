plugins {
    kotlin("jvm") version "2.4.20"
    application
}

group = "org.paintdev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

dependencies {
    implementation("org.jetbrains.jediterm:jediterm-core:3.76")
    implementation("org.jetbrains.jediterm:jediterm-ui:3.76")
    implementation("org.jetbrains.pty4j:pty4j:0.14.0")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("org.paintdev.MainKt")
}

kotlin {
    jvmToolchain(26)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.paintdev.MainKt"
    }
}

tasks.test {
    useJUnitPlatform()
}