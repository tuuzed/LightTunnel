package lighttunnel.util

import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write


internal class IncId {

    private var byteCount = 1.toByte()
    private var data = LinkedList<Byte>()
    private val lock = ReentrantReadWriteLock()

    init {
        data.add(0)
    }

    private fun increment(lastIndex: Int) {
        if (lastIndex == -1) {
            byteCount++
            data.add(1, 0)
            return
        }
        if ((data[lastIndex].toInt() and 0xFF) + 1 > 0xFF) {
            data[lastIndex] = 0
            increment(lastIndex - 1)
        } else {
            data[lastIndex] = (data[lastIndex] + 1).toByte()
        }
    }

    private fun get(): ByteArray = lock.read {
        val result = ByteArray(data.size + 1)
        result[0] = byteCount
        var index = 1
        for (element in data) result[index++] = element
        return result
    }

    private fun incrementAndGet(): ByteArray {
        lock.write { increment(data.lastIndex) }
        return get()
    }

    val nextId get() = incrementAndGet()

}