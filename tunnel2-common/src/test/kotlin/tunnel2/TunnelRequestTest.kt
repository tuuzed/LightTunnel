package tunnel2

import org.junit.Test
import tunnel2.common.TunnelRequest

class TunnelRequestTest {

    @Test
    fun test() {
        val request = TunnelRequest.ofHttp(
            "127.0.0.1",
            80,
            "t1.tunnel.lo",
            proxyAddHeaders = mapOf(Pair("a1", "A1"), Pair("a2", "A1")),
            proxySetHeaders = mapOf(Pair("b1", "B1"), Pair("b2", "B2"))
        )
        println(request)

    }
}