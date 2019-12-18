package lighttunnel.cmd

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.Tag

object Yaml {

    inline fun <reified T> dump(data: T): String = Yaml().dumpAs(data, Tag.MAP, DumperOptions.FlowStyle.BLOCK)

    inline fun <reified T> load(yaml: String): T = Yaml().loadAs(yaml, T::class.java)

}