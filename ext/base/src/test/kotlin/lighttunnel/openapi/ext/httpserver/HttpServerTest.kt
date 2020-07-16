package lighttunnel.openapi.ext.httpserver

import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import org.junit.Before
import org.junit.Test

class HttpServerTest {
    private lateinit var server: HttpServer

    @Before
    fun before() {
        server = HttpServer(
            bossGroup = NioEventLoopGroup(),
            workerGroup = NioEventLoopGroup(),
            bindAddr = "0.0.0.0",
            bindPort = 80
        ) {
            route("/") {
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer("Hello", Charsets.UTF_8)
                )
            }
        }
    }

    @Test
    fun start() {
        server.start()
        Thread.currentThread().join()
    }
}