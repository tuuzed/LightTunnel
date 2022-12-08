plugins {
    kotlin("jvm")
}

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(kotlin("stdlib"))
    api(project(":core:common"))
    testImplementation(project(":extras:logger"))
    testImplementation(Deps.Test.junit)
}

tasks.named("jar").configure {
    val content = """
            app.name: lighttunnel.server
            build.version: ${project.version}
            build.date: ${GitHelper.buildDate}
            commit.date: ${GitHelper.commitDate}
            commit.hash: ${GitHelper.commitHash}
            jks.server.hex: ${rootProject.file("scaffold/certificates/lts.jks").toHex}
            jks.server.store: ltspass
            jks.server.key: ltspass
        """.trimIndent()
    project.file("src/main/resources/lighttunnel/generated/MANIFEST.bin").also {
        it.parentFile.mkdirs()
        it.writeBytes(content.toZip)
    }
}
