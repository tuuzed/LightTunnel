package lighttunnel.cmd

import org.ini4j.Ini
import org.junit.Test

class IniLoadTest {

    @Test
    fun test_iniLoad() {
        val ini = Ini()
        ini.load(this::class.java.getResource("/ltc.ini"))
        val basic = ini["basic"]
        val log = ini["log"]
        val ssl = ini["ssl"]
        println("basic: $basic")
        println("log: $log")
        println("ssl: $ssl")
        val tunnels = ini.entries
            .filter { it.key.startsWith("tunnel") }
            .map { it.value }
        println("tunnels:")
        tunnels.forEach { println("$it") }
    }
}