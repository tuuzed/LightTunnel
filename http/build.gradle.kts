plugins {
    kotlin("jvm")
}

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(kotlin("stdlib"))
    api(project(":common"))
    testImplementation(Deps.Test.junit)
}
