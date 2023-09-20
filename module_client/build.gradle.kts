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
            app.name: lighttunnel.client
            build.version: ${project.version}
            build.date: ${Builds.getBuildDate()}
            commit.date: ${Builds.getCommitDate(project)}
            commit.hash: ${Builds.getCommitHash(project)}
            jks.client.hex: ${rootProject.file(".scaffold/certificates/ltc.jks").toHexString()}
            jks.client.store: ltcpass
        """.trimIndent()
    project.file("src/main/resources/lighttunnel/generated/MANIFEST.BIN").also {
        it.parentFile.mkdirs()
        it.writeBytes(content.compress())
    }
}
