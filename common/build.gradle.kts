plugins {
    kotlin("jvm")
}

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(kotlin("stdlib"))
    api(Deps.Common.netty_buffer)
    api(Deps.Common.netty_codec)
    api(Deps.Common.netty_codec_http)
    api(Deps.Common.netty_common)
    api(Deps.Common.netty_handler)
    api(Deps.Common.netty_resolver)
    api(Deps.Common.netty_transport)
    api(Deps.Common.json)
    api(Deps.Logger.slf4j_api)
    testImplementation(Deps.Test.junit)
}
