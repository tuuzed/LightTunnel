package lighttunnel.common.proto

import io.netty.util.AttributeKey

object Proto {
    private const val PREFIX = "\$lighttunnel.proto"

    const val HDR = 0xFA.toByte()
    const val VERSION = 0x01.toByte()

    val AK_AES128_KEY: AttributeKey<ByteArray?> = AttributeKey.newInstance("$PREFIX.AES128Key")

    const val FLAG_NONE = 0.toByte()

    /** GZIP 压缩 */
    const val FLAG_GZIP = (1 shl 7).toByte()

    /** AES128 加密*/
    const val FLAG_ENCRYPTED = (1 shl 6).toByte()
}
