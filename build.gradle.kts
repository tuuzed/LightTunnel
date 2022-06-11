plugins {
    kotlin("jvm") version "1.6.10"
}

group = "com.github.tuuzed"
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
