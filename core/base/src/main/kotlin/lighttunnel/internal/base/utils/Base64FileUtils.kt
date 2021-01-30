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
        val out = ByteArrayOutputStream()
        val `in` = ByteArrayInputStream(bytes)
        val unzip = GZIPInputStream(`in`)
        val buffer = ByteArray(256)
        var n: Int
        while (unzip.read(buffer).also { n = it } >= 0) {
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
}
