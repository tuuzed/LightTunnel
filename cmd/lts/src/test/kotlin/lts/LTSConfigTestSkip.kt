package lts

import org.junit.Test
import com.tuuzed.lighttunnel.cmd.Yaml

class LTSConfigTestSkip {

    @Test
    fun dump() {
        val config = LTSConfig()
        val yaml = Yaml.dump(config)
        println(yaml)
    }

    @Test
    fun load() {
        val config = Yaml.load<LTSConfig>(javaClass.getResourceAsStream("/lts.yml").reader().readText())

        println(config)
    }
}