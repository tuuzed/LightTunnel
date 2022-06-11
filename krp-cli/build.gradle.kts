plugins {
    kotlin("jvm")
}

group = rootProject.group
version = rootProject.version

project.ext.set("app.name", "krp")

dependencies {
    implementation(kotlin("stdlib"))
    api(project(":common"))
    api(project(":krp"))
    api(project(":httpd"))
    api(project(":log-impl"))
    api(project(":extensions"))
    api(Deps.Command.clikt)
    api(Deps.Config.ini4j)
    testImplementation(Deps.Test.junit)
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
                "Main-Class" to "krp.krp.KrpMainKt",
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
