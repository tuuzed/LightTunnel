package tunnel2.t2cli

import java.io.IOException
import java.util.*

object TunnelManifest {
    private val manifest = LinkedHashMap<String, String>()

    init {
        val url = TunnelManifest::class.java.getResource("/META-INF/MANIFEST.MF")
        try {
            url.openStream().bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    val index = line.indexOf(":")
                    if (index != -1) {
                        val k = line.substring(0, index).trim { it <= ' ' }
                        val v = line.substring(index + 1).trim { it <= ' ' }
                        manifest[k] = v
                    }
                }
            }
        } catch (e: IOException) {
            // pass
        }
    }

    val versionCode = manifest["Tunnel-VersionCode"]
    val versionName = manifest["Tunnel-VersionName"]
    val lastCommitSHA = manifest["Tunnel-LastCommitSHA"]
    val lastCommitDate = manifest["Tunnel-LastCommitDate"]
    val buildDate = manifest["Tunnel-BuildDate"]
}
