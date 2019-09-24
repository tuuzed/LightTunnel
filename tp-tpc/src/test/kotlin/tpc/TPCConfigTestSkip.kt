package tpc

import org.junit.Test
import org.yaml.snakeyaml.DumperOptions
import tpstarter.Yaml

class TPCConfigTestSkip {

    @Test
    fun dump() {
        val config = TPCConfig().also {
            it.tunnels = listOf(TPCConfig.Tunnel(), TPCConfig.Tunnel())
        }
        val yaml = Yaml.dump(config)
        println(yaml)
    }

    @Test
    fun load() {
        val config = Yaml.load<TPCConfig>(
            javaClass.getResourceAsStream("/tpc.yml").reader(Charsets.UTF_8).readText()
        )
        println(config)
    }
}