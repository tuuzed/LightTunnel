package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import lighttunnel.proto.TunnelRequest
import lighttunnel.server.util.IncIds
import java.util.concurrent.ConcurrentHashMap

class SessionChannels(
    val tunnelId: Long,
    val request: TunnelRequest,
    val tunnelChannel: Channel
) {
    private val ids = IncIds()
    private val cachedChannels = ConcurrentHashMap<Long, Channel>()

    fun putChannel(channel: Channel): Long {
        val sessionId = ids.nextId
        synchronized(cachedChannels) {
            cachedChannels.put(sessionId, channel)
        }
        return sessionId
    }

    fun getChannel(sessionId: Long): Channel? {
        synchronized(cachedChannels) {
            return cachedChannels[sessionId]
        }
    }

    fun removeChannel(sessionId: Long): Channel? {
        synchronized(cachedChannels) {
            return cachedChannels.remove(sessionId)
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
