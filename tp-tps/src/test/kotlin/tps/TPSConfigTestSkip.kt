package tps

import org.junit.Test
import tpstarter.Yaml

class TPSConfigTestSkip {

    @Test
    fun dump() {
        val config = TPSConfig()
        val yaml = Yaml.dump(config)
        println(yaml)
    }

    @Test
    fun load() {
        val config = Yaml.load<TPSConfig>(
            javaClass.getResourceAsStream("/tps.yml").reader(Charsets.UTF_8).readText()
        )
        println(config)
    }
}