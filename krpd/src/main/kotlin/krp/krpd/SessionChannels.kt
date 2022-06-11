package krp.krpd

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import krp.common.entity.TunnelRequest
import krp.common.proto.msg.ProtoMsgForceOff
import krp.common.utils.IncIds
import krp.krpd.traffic.DefaultTrafficStats
import java.util.concurrent.ConcurrentHashMap

internal class SessionChannels(
    val tunnelId: Long,
    val tunnelRequest: TunnelRequest,
    val tunnelChannel: Channel,
) {

    private val ids = IncIds()
    private val cachedSessionIdChannels = ConcurrentHashMap<Long, Channel>()

    val trafficStats = DefaultTrafficStats()
    val cachedChannelCount: Int get() = cachedSessionIdChannels.count()

    fun putSessionChannel(channel: Channel): Long {
        val sessionId = ids.nextId
        cachedSessionIdChannels[sessionId] = channel
        return sessionId
    }

    fun getSessionChannel(sessionId: Long): Channel? = cachedSessionIdChannels[sessionId]

    fun removeSessionChannel(sessionId: Long): Channel? = cachedSessionIdChannels.remove(sessionId)

    fun writeAndFlushForceOffMsg() {
        tunnelChannel.writeAndFlush(ProtoMsgForceOff)
    }

    fun depose() {
        cachedSessionIdChannels.forEach { (_, ch) ->
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
        cachedSessionIdChannels.clear()
    }

}
