package tpcommon


enum class TPType(val value: Byte) {
    UNKNOWN(0x00.toByte()),
    TCP(0x10.toByte()),
    HTTP(0x30.toByte()),
    HTTPS(0x31.toByte());

    companion object {
        @JvmStatic
        fun ofValue(value: Byte) = values().firstOrNull { it.value == value } ?: UNKNOWN
    }

}