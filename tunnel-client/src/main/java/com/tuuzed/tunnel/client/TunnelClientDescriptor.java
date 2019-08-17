package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.client.internal.AttributeKeys;
import com.tuuzed.tunnel.common.proto.ProtoMessage;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.common.util.Function1;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class TunnelClientDescriptor {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClient.class);


    @NotNull
    private final Bootstrap bootstrap;
    @NotNull
    private final String serverAddr;
    private final int serverPort;
    @NotNull
    private final ProtoRequest protoRequest;
    @NotNull
    private final AtomicBoolean shutdownFlag = new AtomicBoolean(false);
    @Nullable
    private ChannelFuture connectChannelFuture;

    TunnelClientDescriptor(
        @NotNull final Bootstrap bootstrap,
        @NotNull final String serverAddr,
        final int serverPort,
        @NotNull final ProtoRequest protoRequest
    ) {
        this.bootstrap = bootstrap;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.protoRequest = protoRequest;
    }

    void connect(@NotNull final Function1<TunnelClientDescriptor> failureCallback) {
        if (shutdownFlag.get()) {
            logger.warn("This tunnel already shutdown.");
            return;
        }
        ChannelFuture f = bootstrap.connect(serverAddr, serverPort);
        connectChannelFuture = f;
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    // 连接成功，向服务器发送请求建立隧道消息
                    future.channel().writeAndFlush(
                        new ProtoMessage(
                            ProtoMessage.Type.REQUEST,
                            protoRequest.toBytes(),
                            null
                        )
                    );
                    future.channel().attr(AttributeKeys.TUNNEL_CLIENT_DESCRIPTOR).set(TunnelClientDescriptor.this);
                } else {
                    failureCallback.invoke(TunnelClientDescriptor.this);
                }
            }
        });
    }

    @NotNull
    public String serverAddr() {
        return serverAddr;
    }

    public int serverPort() {
        return serverPort;
    }

    @NotNull
    public ProtoRequest protoRequest() {
        return protoRequest;
    }

    public boolean isShutdown() {
        return shutdownFlag.get();
    }

    public void shutdown() {
        if (connectChannelFuture != null) {
            shutdownFlag.set(true);
            connectChannelFuture.channel().attr(AttributeKeys.TUNNEL_CLIENT_DESCRIPTOR).set(null);
            connectChannelFuture.channel().close();
        }
    }

    @Override
    public String toString() {
        return protoRequest.toString();
    }
}
