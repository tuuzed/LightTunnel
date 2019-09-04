package tunnel2.server.udp

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOption
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel
import org.junit.Test
import java.nio.charset.StandardCharsets

class UdpServerExclude {
    @Test
    fun run() {
        val b = Bootstrap()
        val group = NioEventLoopGroup()
        b.group(group)
            .channel(NioDatagramChannel::class.java)
            .option(ChannelOption.SO_BROADCAST, true)
            .handler(UdpServerHandler())
        b.bind(2555).sync().channel().closeFuture().await()
    }

    private class UdpServerHandler : SimpleChannelInboundHandler<DatagramPacket>() {

        @Throws(Exception::class)
        override fun channelRead0(ctx: ChannelHandlerContext, packet: DatagramPacket) {
            val buf = packet.copy().content()
            val req = ByteArray(buf.readableBytes())
            buf.readBytes(req)
            val body = String(req, StandardCharsets.UTF_8)
            println(body)//打印收到的信息
            //向客户端发送消息
            val text = "来自服务端: World"
            // 由于数据报的数据是以字符数组传的形式存储的，所以传转数据
            val bytes = text.toByteArray(StandardCharsets.UTF_8)
            val data = DatagramPacket(Unpooled.copiedBuffer(bytes), packet.sender())
            ctx.writeAndFlush(data)//向客户端发送消息
        }

    }

}
