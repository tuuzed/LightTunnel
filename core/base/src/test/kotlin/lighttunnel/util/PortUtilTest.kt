package lighttunnel.util

import org.junit.Test

class PortUtilTest {

    @Test
    fun getRandomPort() {
        for (i in 1..10000) {
            val port = PortUtil.getAvailableTcpPort("4000-21000,30000,30001,30003")
            print("$port,")
            if (i != 0 && i % 10 == 0) {
                println()
            }
        }
    }

}