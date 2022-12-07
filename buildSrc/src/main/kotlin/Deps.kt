/** 依赖 */
object Deps {

    object Common {
        private const val netty_version = "4.1.85.Final"

        /** NIO网络库 */
        const val netty_buffer = "io.netty:netty-buffer:$netty_version"
        const val netty_codec = "io.netty:netty-codec:$netty_version"
        const val netty_codec_http = "io.netty:netty-codec-http:$netty_version"
        const val netty_common = "io.netty:netty-common:$netty_version"
        const val netty_handler = "io.netty:netty-handler:$netty_version"
        const val netty_resolver = "io.netty:netty-resolver:$netty_version"
        const val netty_transport = "io.netty:netty-transport:$netty_version"

        /** Json */
        const val json = "org.json:json:20220924"
    }

    object Command {
        /** 命令行参数解析 */
        const val clikt = "com.github.ajalt.clikt:clikt:3.5.0"
    }

    /** 日志库 */
    object Logger {
        const val slf4j_api = "org.slf4j:slf4j-api:1.7.36"
        const val slf4j_log4j12 = "org.slf4j:slf4j-log4j12:1.7.36"
    }

    object Config {
        /** ini配置文件解析 */
        const val ini4j = "org.ini4j:ini4j:0.5.4"
    }

    /** 单元测试 */
    object Test {
        const val junit = "junit:junit:4.13.2"
    }

}
