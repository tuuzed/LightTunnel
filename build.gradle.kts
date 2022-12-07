plugins {
    kotlin("jvm") version "1.7.22"
}

group = "lighttunnel"
version = "0.0.1"

allprojects {
    repositories {
        mavenCentral()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}
