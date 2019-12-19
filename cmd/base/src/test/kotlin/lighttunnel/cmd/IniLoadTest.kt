package lighttunnel.cmd

import org.ini4j.Ini
import org.junit.Test

class IniLoadTest {

    @Test
    fun test_iniLoad() {
        val ini = Ini()
        ini.load(this::class.java.getResource("/ltc.ini"))
        val basic = ini["basic"]
        println("basic: $basic")
        val tunnels = ini.entries
            .filterNot { it.key == "basic" }
            .map { it.value }
        println("tunnels:")
        tunnels.forEach { println("$it") }
    }
}