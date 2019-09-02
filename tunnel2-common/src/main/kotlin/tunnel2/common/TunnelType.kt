package tunnel2.common

enum class TunnelType(val value: Byte) {
    UNKNOWN(0x00.toByte()),
    TCP(0x01.toByte()),
    UDP(0x01.toByte()),
    HTTP(0x02.toByte()),
    HTTPS(0x03.toByte());

    companion object {
        @JvmStatic
        fun ofValue(value: Byte): TunnelType = values().firstOrNull { it.value == value } ?: UNKNOWN
    }
}