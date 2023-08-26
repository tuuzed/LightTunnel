plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    api(project(":core:common"))
    testImplementation(Libs.junit)
}
