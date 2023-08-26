package lighttunnel.common.extensions

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import lighttunnel.common.utils.CompressUtils
import lighttunnel.common.utils.CryptoUtils
import java.io.IOException

/** 空字节数组 */
val emptyBytes = ByteArray(0)

fun Short.toByteArray(start: Int = 0, dst: ByteArray = ByteArray(2)): ByteArray {
    for (i in 0 until 2) {
        dst[start + i] = (this.toInt() shr 8 * (2 - 1 - i) and 0xFF).toByte()
    }
    return dst
}

fun Int.toByteArray(start: Int = 0, dst: ByteArray = ByteArray(4)): ByteArray {
    for (i in 0 until 4) {
        dst[start + i] = (this shr 8 * (4 - 1 - i) and 0xFF).toByte()
    }
    return dst
}

fun Long.toByteArray(start: Int = 0, dst: ByteArray = ByteArray(8)): ByteArray {
    for (i in 0 until 8) {
        dst[start + i] = (this shr 8 * (8 - 1 - i) and 0xFF).toByte()
    }
    return dst
}

fun ByteBuf.toByteArray(): ByteArray {
    return ByteBufUtil.getBytes(this)
}

@Throws(IOException::class)
fun ByteArray.tryCompress(): Pair<Boolean, ByteArray> {
    if (this.size > 1024) {
        val gzip = CompressUtils.compress(this)
        return if (gzip.size >= this.size) (false to this) else (true to gzip)
    }
    return false to this
}

@Throws(Exception::class)
fun ByteArray.tryEncryptAES128(key: ByteArray) = CryptoUtils.encryptAES128(this, key)

@Throws(Exception::class)
fun ByteArray.tryDecryptAES128(key: ByteArray) = CryptoUtils.decryptAES128(this, key)
