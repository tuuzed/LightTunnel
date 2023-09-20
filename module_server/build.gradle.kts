plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    api(project(":module_common"))
    testImplementation(project(":module_logger"))
    testImplementation(Libs.junit)
}

tasks.named("jar").configure {
    val content = """
            app.name: lighttunnel.server
            build.version: ${project.version}
            build.date: ${Builds.getBuildDate()}
            commit.date: ${Builds.getCommitDate(project)}
            commit.hash: ${Builds.getCommitHash(project)}
            jks.server.hex: ${rootProject.file(".scaffold/certificates/lts.jks").toHexString()}
            jks.server.store: ltspass
            jks.server.key: ltspass
        """.trimIndent()
    project.file("src/main/resources/lighttunnel/generated/MANIFEST.BIN").also {
        it.parentFile.mkdirs()
        it.writeBytes(content.compress())
    }
}
