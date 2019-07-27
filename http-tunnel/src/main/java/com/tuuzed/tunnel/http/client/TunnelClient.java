package com.tuuzed.tunnel.http.client;

import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import com.tuuzed.tunnel.common.protocol.TunnelMessageDecoder;
import com.tuuzed.tunnel.common.protocol.TunnelMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.jetbrains.annotations.NotNull;

public class TunnelClient {

    private final NioEventLoopGroup workerGroup;

    public TunnelClient(@NotNull NioEventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
    }

    public void start(@NotNull final String address, final int port,
                      @NotNull final String subDomain,
                      @NotNull final String loacalAddr, final int localPort
    ) throws Exception {
        final Bootstrap bootstrap = new Bootstrap();
        final LocalConnect localConnect = new LocalConnect(workerGroup);
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new TunnelMessageDecoder())
                                .addLast(new TunnelMessageEncoder())
                                .addLast(new TunnelClientChannelHandler(localConnect))
                        ;
                    }
                });
        ChannelFuture f = bootstrap.connect(address, port).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    byte[] requestBytes = OpenTunnelRequest.httpBuilder(subDomain)
                            .setLocalAddr(loacalAddr)
                            .setLocalPort(localPort)
                            .build().toBytes();
                    future.channel().writeAndFlush(
                            TunnelMessage.newInstance(TunnelMessage.MESSAGE_TYPE_OPEN_TUNNEL_REQUEST)
                                    .setHead(requestBytes)
                    );
                }
            }
        });
        f.get();
    }
}
