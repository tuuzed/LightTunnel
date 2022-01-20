package lighttunnel.base

enum class TunnelType(val value: Byte) {
    UNKNOWN(0x00.toByte()),
    TCP(0x10.toByte()),
    HTTP(0x30.toByte()),
    HTTPS(0x31.toByte()),
    ;

    companion object {
        private val mappings = values().associateBy { it.value }

        @JvmStatic
        fun findTunnelType(value: Byte) = mappings.getOrDefault(value, UNKNOWN)
    }

}
