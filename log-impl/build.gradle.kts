plugins {
    kotlin("jvm")
}

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(kotlin("stdlib"))
    api(project(":common"))
    api(Deps.Logger.slf4j_log4j12)
    testImplementation(Deps.Test.junit)
}
