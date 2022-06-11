package krp.common.utils

object ManifestUtils {

    private val text: String?
        get() = this::class.java.getResourceAsStream("/generated/MANIFEST")
            ?.use { it.readBytes() }
            ?.let { String(unGZip(it)) }

    private val infos: Map<String, String> by lazy {
        text?.lines()?.associate { it.split(": ").let { pair -> pair.first() to pair.last() } } ?: emptyMap()
    }

    val appName: String get() = infos["app.name"] ?: "unknown"
    val version: String get() = infos["build.version"] ?: "unknown"
    val buildDate: String get() = infos["build.date"] ?: "unknown"
    val commitDate: String get() = infos["commit.date"] ?: "unknown"
    val commitHash: String get() = infos["commit.hash"] ?: "unknown"

    val jksServerHex: String get() = infos["jks.server.hex"] ?: ""
    val jksServerStore: String get() = infos["jks.server.store"] ?: ""
    val jksServerKey: String get() = infos["jks.server.key"] ?: ""

    val jksClientHex: String get() = infos["jks.client.hex"] ?: ""
    val jksClientStore: String get() = infos["jks.client.store"] ?: ""
}
