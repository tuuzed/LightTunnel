package ltc

import com.tuuzed.lighttunnel.cmd.Yaml
import org.junit.Test

class LTCConfigTestSkip {

    @Test
    fun dump() {
        val config = LTCConfig().also {
            it.tunnels = listOf(LTCConfig.Tunnel(), LTCConfig.Tunnel())
        }
        val yaml = Yaml.dump(config)
        println(yaml)
    }

    @Test
    fun load() {
        val config = Yaml.load<LTCConfig>(javaClass.getResourceAsStream("/ltc.yml").reader().readText())
        println(config)
    }
}