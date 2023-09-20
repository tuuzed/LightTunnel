plugins {
    kotlin("jvm")
}

project.ext.set("app.name", "ltc")

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":app_base"))
    implementation(project(":module_client"))
    implementation(project(":module_httpserver"))
    implementation(project(":module_logger"))
    implementation(Libs.clikt)
    testImplementation(Libs.junit)
}

tasks.register("fatJar", FatJarTask::class) {
    dependsOn.add(tasks.named("jar"))
    exclude("META-INF/**")
    exclude("**/*.kotlin_module")
    exclude("**/*.kotlin_metadata")
    exclude("**/*.kotlin_builtins")
    manifest {
        attributes(
            mapOf(
                "Main-Class" to "lighttunnel.ltc.MainKt",
            )
        )
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    from(sourceSets.main.get().output)
}

tasks.register("r8Jar", R8JarTask::class) {
    dependsOn.add(tasks.named("fatJar"))
}

tasks.register("binaryJar", BinaryJarTask::class) {
    dependsOn.add(tasks.named("r8Jar"))
}
tasks.register("publish", PublishTask::class) {
    dependsOn.add(tasks.named("binaryJar"))
    appName = project.ext.get("app.name").toString()
}
