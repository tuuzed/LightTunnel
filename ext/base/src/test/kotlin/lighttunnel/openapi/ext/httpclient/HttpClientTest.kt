package lighttunnel.openapi.ext.httpclient

import io.netty.buffer.ByteBufUtil
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.HttpMethod
import org.junit.Before
import org.junit.Test

class HttpClientTest {

    lateinit var client: HttpClient

    @Before
    fun setUp() {
        client = HttpClient(NioEventLoopGroup())
    }

    @Test
    fun request() {
        client.request("http://localhost", HttpMethod.GET) {
            println(ByteBufUtil.getBytes(it.content())?.contentToString())
        }
        Thread.currentThread().join(3000)
    }

}