package tunnel2.server.internal

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import tunnel2.common.TunnelRequest
import java.util.concurrent.ConcurrentHashMap

class ServerSessionChannels(
    val tunnelId: Long,
    val tunnelRequest: TunnelRequest,
    val tunnelChannel: Channel
) {
    private val sessionIdProducer: IdProducer = IdProducer()
    private val cachedSessionChannels = ConcurrentHashMap<Long, Channel>()

    fun putSessionChannel(channel: Channel): Long {
        val sessionToken = sessionIdProducer.nextId
        synchronized(cachedSessionChannels) {
            cachedSessionChannels.put(sessionToken, channel)
        }
        return sessionToken
    }

    fun getSessionChannel(sessionToken: Long): Channel? {
        synchronized(cachedSessionChannels) {
            return cachedSessionChannels[sessionToken]
        }
    }

    fun removeSessionChannel(sessionToken: Long): Channel? {
        synchronized(cachedSessionChannels) {
            return cachedSessionChannels.remove(sessionToken)
        }
    }

    fun destroy() {
        synchronized(cachedSessionChannels) {
            val channels = cachedSessionChannels.values
            for (ch in channels) {
                ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            }
            cachedSessionChannels.clear()
        }
    }

}
