plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    api(project(":module_common"))
    testImplementation(Libs.junit)
}
