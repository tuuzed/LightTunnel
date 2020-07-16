package lighttunnel.openapi

enum class TunnelType(val code: Byte) {
    UNKNOWN(0x00.toByte()),
    TCP(0x10.toByte()),
    HTTP(0x30.toByte()),
    HTTPS(0x31.toByte());

    companion object {
        @JvmStatic
        fun codeOf(code: Byte) = values().firstOrNull { it.code == code } ?: UNKNOWN
    }

}