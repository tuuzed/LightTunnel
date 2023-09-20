plugins {
    kotlin("jvm") version "1.9.23"
}

group = "lighttunnel"
version = "0.0.1"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}
