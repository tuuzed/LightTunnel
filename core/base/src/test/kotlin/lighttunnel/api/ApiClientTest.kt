package lighttunnel.api

import io.netty.buffer.ByteBufUtil
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpMethod
import org.junit.Before
import org.junit.Test

class ApiClientTest {

    lateinit var client: ApiClient

    @Before
    fun setUp() {
        client = ApiClient(NioEventLoopGroup())
    }

    @Test
    fun request() {
        client.request("http://localhost", HttpMethod.GET,
            object : ApiClient.ResponseCallback {
                override fun onResponse(response: FullHttpResponse) {
                    println(ByteBufUtil.getBytes(response.content())!!.contentToString())
                }
            }
        )
        Thread.currentThread().join(3000)
    }
}