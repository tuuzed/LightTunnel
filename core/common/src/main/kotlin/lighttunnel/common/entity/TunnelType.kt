package lighttunnel.common.entity

enum class TunnelType(val value: Int) {
    UNKNOWN(0x00),
    TCP(0x01),
    HTTP(0x02),
    HTTPS(0x03),
    ;

    companion object {
        private val mappings = values().associateBy { it.value }

        @JvmStatic
        fun findTunnelType(value: Int) = mappings.getOrDefault(value, UNKNOWN)
    }

}
