package lighttunnel.server.util

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.util.IncIds
import lighttunnel.openapi.TunnelRequest
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class SessionChannels(
    val tunnelId: Long,
    val tunnelRequest: TunnelRequest,
    val tunnelChannel: Channel
) {

    private val ids = IncIds()
    private val cachedSessionIdChannels = hashMapOf<Long, Channel>()
    private val lock = ReentrantReadWriteLock()

    val statistics = StatisticsDefaultImpl()
    val cachedChannelCount: Int get() = lock.read { cachedSessionIdChannels.count() }

    fun putChannel(channel: Channel): Long {
        val sessionId = ids.nextId
        lock.write { cachedSessionIdChannels[sessionId] = channel }
        return sessionId
    }

    fun getChannel(sessionId: Long): Channel? = lock.read { cachedSessionIdChannels[sessionId] }

    fun removeChannel(sessionId: Long): Channel? = lock.write { cachedSessionIdChannels.remove(sessionId) }

    fun forceOff() {
        tunnelChannel.writeAndFlush(ProtoMessage.FORCE_OFF())
    }

    fun depose() = lock.write {
        cachedSessionIdChannels.forEach { (_, ch) ->
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
        cachedSessionIdChannels.clear()
    }

}
