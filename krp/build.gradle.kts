plugins {
    kotlin("jvm")
}

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(kotlin("stdlib"))
    api(project(":common"))
    testImplementation(project(":log-impl"))
    testImplementation(Deps.Test.junit)
}

tasks.named("jar").configure {
    val content = """
            app.name: krp
            build.version: ${project.version}
            build.date: $buildDate
            commit.date: $commitDate
            commit.hash: $commitHash
            jks.client.hex: ${rootProject.file("scaffold/certificates/krp.jks").hex}
            jks.client.store: krppass
        """.trimIndent()
    project.file("src/main/resources/generated/MANIFEST").also {
        it.parentFile.mkdirs()
        it.writeBytes(content.zip)
    }
}
