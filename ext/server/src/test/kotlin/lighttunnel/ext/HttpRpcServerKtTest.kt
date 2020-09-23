package lighttunnel.ext

import org.junit.Test

class HttpRpcServerKtTest {


    @Test
    fun test() {
        val r = "^/api/.*".toRegex()
        val r2 = "^/api/status".toRegex()
        println(r.matches("/api/status/dfasdf?asdf=aadf"))
        println(r.matches("/api/status"))
        println(r2.matches("/api/status"))
    }
}