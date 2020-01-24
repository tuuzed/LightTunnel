package lighttunnel.server.util

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import lighttunnel.proto.TunnelRequest
import java.util.concurrent.ConcurrentHashMap

class SessionChannels(
    val tunnelId: Long,
    val tunnelRequest: TunnelRequest,
    val tunnelChannel: Channel
) {
    private val ids = IncIds()
    private val cachedChannels = ConcurrentHashMap<Long, Channel>()

    val cachedChannelCount: Int
        get() {
            return cachedChannels.count()
        }

    fun putChannel(channel: Channel): Long {
        val sessionId = ids.nextId
        cachedChannels[sessionId] = channel
        return sessionId
    }

    fun getChannel(sessionId: Long): Channel? {
        return cachedChannels[sessionId]
    }

    fun removeChannel(sessionId: Long): Channel? {
        return cachedChannels.remove(sessionId)
    }

    fun destroy() {
        cachedChannels.forEach { (_, ch) ->
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
        cachedChannels.clear()
    }
}
