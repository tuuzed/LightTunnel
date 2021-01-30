package lighttunnel.internal.base.utils

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream

object Base64FileUtils {

    fun decode(base64: Array<String>, unzip: Boolean = false): ByteArray {
        val buf = Unpooled.wrappedBuffer(base64.joinToString("").toByteArray())
        var data = ByteBufUtil.getBytes(Base64.decode(buf))
        if (unzip && data.isEmpty()) {
            try {
                data = unzip(data)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return data
    }

    @Throws(IOException::class)
    private fun unzip(bytes: ByteArray): ByteArray {
        val buffer = ByteArrayOutputStream()
        val unzip = GZIPInputStream(ByteArrayInputStream(bytes))
        val buf = ByteArray(256)
        var len: Int
        while (unzip.read(buf).also { len = it } >= 0) {
            buffer.write(buf, 0, len)
        }
        return buffer.toByteArray()
    }
}
