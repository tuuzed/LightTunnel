plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    api(Libs.netty_buffer)
    api(Libs.netty_codec)
    api(Libs.netty_codec_http)
    api(Libs.netty_common)
    api(Libs.netty_handler)
    api(Libs.netty_resolver)
    api(Libs.netty_transport)
    api(Libs.org_json)
    api(Libs.slf4j_api)
    testImplementation(Libs.junit)
}
