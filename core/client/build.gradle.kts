plugins {
    kotlin("jvm")
}

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(kotlin("stdlib"))
    api(project(":core:common"))
    testImplementation(project(":extras:logimpl"))
    testImplementation(Deps.Test.junit)
}

tasks.named("jar").configure {
    val content = """
            app.name: lighttunnel.client
            build.version: ${project.version}
            build.date: ${GitHelper.buildDate}
            commit.date: ${GitHelper.commitDate}
            commit.hash: ${GitHelper.commitHash}
            jks.client.hex: ${rootProject.file("scaffold/certificates/ltc.jks").toHex}
            jks.client.store: ltcpass
        """.trimIndent()
    project.file("src/main/resources/lighttunnel/generated/MANIFEST.bin").also {
        it.parentFile.mkdirs()
        it.writeBytes(content.toZip)
    }
}
