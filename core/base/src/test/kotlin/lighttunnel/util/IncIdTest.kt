package lighttunnel.util

import org.junit.Test
import java.util.*

class IncIdTest {
    @Test
    fun nextId() {
        val incId = IncId()
        val sb = StringBuffer()
        for (i in 0 until 65535) {
            sb.append(i).append("=>")
            sb.append(bytesToHexString(incId.nextId))
            sb.append("\n")
        }
        println(sb)
        Thread.currentThread().join()
    }

    fun bytesToHexString(bytes: ByteArray?): String {
        if (null == bytes) return ""
        val strBuilder = StringBuilder()
        for (b in bytes) {
            val hex = Integer.toHexString(0xFF and b.toInt())
            if (1 == hex.length) {
                strBuilder.append("0")
            }
            strBuilder.append(hex.toUpperCase(Locale.ENGLISH))
        }
        return strBuilder.toString()
    }
}