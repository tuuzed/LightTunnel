package lighttunnel.http.client

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslHandler
import io.netty.util.AttributeKey
import java.net.URI

class HttpClient(
    workerGroup: NioEventLoopGroup,
    private val sslContext: SslContext? = null,
    private val maxContentLength: Int = 512 * 1024
) {
    companion object {
        internal val REQUEST_CALLBACK: AttributeKey<(response: FullHttpResponse) -> Unit> =
            AttributeKey.newInstance("lighttunnel.http.client.REQUEST_CALLBACK")
    }

    private val bootstrap = Bootstrap()

    init {
        bootstrap
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel?) {
                    ch ?: return
                    if (sslContext != null) {
                        ch.pipeline().addFirst(
                            "ssl", SslHandler(sslContext.newEngine(ch.alloc()))
                        )
                    }
                    ch.pipeline()
                        .addLast("codec", HttpClientCodec())
                        .addLast("aggregator", HttpObjectAggregator(maxContentLength))
                        .addLast("handler", HttpClientChannelHandler())
                }

            })
    }

    @Throws(Exception::class)
    fun request(url: String, method: HttpMethod, content: ByteBuf = Unpooled.EMPTY_BUFFER, callback: (response: FullHttpResponse) -> Unit) {
        val uri = URI.create(url)
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, url, content)
        bootstrap.connect(uri.path, if (uri.port == -1) {
            if ("https".equals(uri.scheme, true)) 443 else 80
        } else {
            uri.port
        }).sync().channel().also {
            it.attr(REQUEST_CALLBACK).set(callback)
        }.writeAndFlush(request)
    }

}