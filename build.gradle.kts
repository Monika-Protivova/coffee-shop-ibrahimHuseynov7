plugins {
    kotlin("jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.7"
}

group = "com.motycka.edu"
version = "0.0.1"

application {
    mainClass = "com.motycka.edu.ApplicationKt" // Corrected main class
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}
