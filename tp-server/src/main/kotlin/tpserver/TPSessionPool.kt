package tpserver

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import tpcommon.TPRequest
import java.util.concurrent.ConcurrentHashMap

class TPSessionPool(
    val tunnelId: Long,
    val tpRequest: TPRequest,
    val tpChannel: Channel
) {
    private val ids = TPIds()
    private val cachedChannels = ConcurrentHashMap<Long, Channel>()

    fun putChannel(channel: Channel): Long {
        val sessionToken = ids.nextId
        synchronized(cachedChannels) {
            cachedChannels.put(sessionToken, channel)
        }
        return sessionToken
    }

    fun getChannel(sessionToken: Long): Channel? {
        synchronized(cachedChannels) {
            return cachedChannels[sessionToken]
        }
    }

    fun removeChannel(sessionToken: Long): Channel? {
        synchronized(cachedChannels) {
            return cachedChannels.remove(sessionToken)
        }
    }

    fun destroy() {
        synchronized(cachedChannels) {
            val channels = cachedChannels.values
            for (ch in channels) {
                ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            }
            cachedChannels.clear()
        }
    }

}
