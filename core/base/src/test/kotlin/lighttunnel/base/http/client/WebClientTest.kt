package lighttunnel.base.http.client

import io.netty.buffer.ByteBufUtil
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.HttpMethod
import org.junit.Before
import org.junit.Test

class WebClientTest {

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