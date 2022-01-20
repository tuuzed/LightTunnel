package lighttunnel.base.entity

enum class TunnelType(val value: Int) {
    UNKNOWN(0x00),
    TCP(0x10),
    HTTP(0x20),
    HTTPS(0x21),
    ;

    companion object {
        private val mappings = values().associateBy { it.value }

        @JvmStatic
        fun findTunnelType(value: Int) = mappings.getOrDefault(value, UNKNOWN)
    }

}
