package lighttunnel.common.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


object CompressUtils {

    @Throws(IOException::class)
    fun gzip(data: ByteArray): ByteArray {
        val buffer = ByteArrayOutputStream()
        val zipStream = GZIPOutputStream(buffer)
        zipStream.write(data)
        zipStream.close()
        return buffer.toByteArray()
    }

    @Throws(IOException::class)
    fun unGZip(data: ByteArray): ByteArray {
        val buffer = ByteArrayOutputStream()
        val unzip = GZIPInputStream(ByteArrayInputStream(data))
        val buf = ByteArray(256)
        var len: Int
        while (unzip.read(buf).also { len = it } >= 0) {
            buffer.write(buf, 0, len)
        }
        return buffer.toByteArray()
    }

}