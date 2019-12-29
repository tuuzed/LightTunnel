package lighttunnel.api

import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.*
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

class ApiServerTest {
    lateinit var server: ApiServer

    @Before
    fun setUp() {
        server = ApiServer(
            NioEventLoopGroup(),
            NioEventLoopGroup(),
            null,
            80,
            object : ApiServer.RequestDispatcher {
                override fun doRequest(request: FullHttpRequest): FullHttpResponse {
                    return DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer("Hello", Charsets.UTF_8)
                    )
                }
            })
    }

    @Test
    fun start() {
        server.start()
        Thread.currentThread().join()
    }
}