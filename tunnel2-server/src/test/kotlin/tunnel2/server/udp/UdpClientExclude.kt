package tunnel2.server.udp

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.util.CharsetUtil
import org.junit.Test
import java.net.InetSocketAddress

class UdpClientExclude {


    @Test
    fun run() {
        val b = Bootstrap()
            .group(NioEventLoopGroup())
            .channel(NioDatagramChannel::class.java)
            .handler(ClientHandler())
        val ch = b.bind(0).sync().channel()
        ch.writeAndFlush(
            DatagramPacket(
                Unpooled.copiedBuffer("来自客户端:Hello", CharsetUtil.UTF_8),
                InetSocketAddress("127.0.0.1", 2555)
            )
        ).sync()
        ch.closeFuture().await()
    }

    private class ClientHandler : SimpleChannelInboundHandler<DatagramPacket>() {

        @Throws(Exception::class)
        override fun channelRead0(ctx: ChannelHandlerContext, packet: DatagramPacket) {
            val body = packet.content().toString(CharsetUtil.UTF_8)
            println(body)
        }
    }


}
