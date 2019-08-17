package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.client.internal.AttributeKeys;
import com.tuuzed.tunnel.client.local.LocalConnect;
import com.tuuzed.tunnel.common.proto.ProtoMessage;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;


/**
 * Tunnel客户端数据通道处理器
 */
@SuppressWarnings("Duplicates")
class TunnelClientChannelHandler extends SimpleChannelInboundHandler<ProtoMessage> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClientChannelHandler.class);
    @NotNull
    private ChannelListener channelListener;
    @NotNull
    private final LocalConnect localConnect;

    public TunnelClientChannelHandler(@NotNull LocalConnect localConnect, @NotNull ChannelListener channelListener) {
        this.localConnect = localConnect;
        this.channelListener = channelListener;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 隧道断开
        final Long tunnelToken = ctx.channel().attr(AttributeKeys.TUNNEL_TOKEN).get();
        final Long sessionToken = ctx.channel().attr(AttributeKeys.SESSION_TOKEN).get();
        if (tunnelToken != null && sessionToken != null) {
            Channel localChannel = localConnect.removeLocalChannel(tunnelToken, sessionToken);
            if (localChannel != null) {
                localChannel.close();
            }
        }
        super.channelInactive(ctx);
        channelListener.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.trace("exceptionCaught: ", cause);
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtoMessage msg) throws Exception {
        logger.trace("channelRead0 : {}", msg);
        switch (msg.getType()) {
            case HEARTBEAT_PING:
                handlePingMessage(ctx, msg);
                break;
            case RESPONSE:
                handleResponseMessage(ctx, msg);
                break;
            case TRANSFER:
                handleTransferMessage(ctx, msg);
                break;
            case REMOTE_CONNECTED:
                handleRemoteConnectedMessage(ctx, msg);
                break;
            case REMOTE_DISCONNECT:
                handleRemoteDisconnectMessage(ctx, msg);
                break;
            default:
                break;
        }
    }


    private void handlePingMessage(ChannelHandlerContext ctx, ProtoMessage msg) throws Exception {
        ctx.writeAndFlush(new ProtoMessage(ProtoMessage.Type.HEARTBEAT_PONG, null, null));
    }

    private void handleResponseMessage(ChannelHandlerContext ctx, ProtoMessage msg) throws Exception {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final boolean success = head.readBoolean();
        if (success) {
            // 开启隧道成功
            final long tunnelToken = head.readLong();
            final ProtoRequest protoRequest = ProtoRequest.fromBytes(msg.getData());
            ctx.channel().attr(AttributeKeys.TUNNEL_TOKEN).set(tunnelToken);
            ctx.channel().attr(AttributeKeys.PROTO_REQUEST).set(protoRequest);
            ctx.channel().attr(AttributeKeys.FATAL_FLAG).set(null);
            ctx.channel().attr(AttributeKeys.FATAL_CAUSE).set(null);
            logger.info("Opened Tunnel: {}", protoRequest);
            channelListener.tunnelConnected(ctx);
        } else {
            // 开启隧道失败
            final String fatalMessage = new String(msg.getData(), StandardCharsets.UTF_8);
            ctx.channel().attr(AttributeKeys.TUNNEL_TOKEN).set(null);
            ctx.channel().attr(AttributeKeys.PROTO_REQUEST).set(null);
            ctx.channel().attr(AttributeKeys.FATAL_FLAG).set(true);
            ctx.channel().attr(AttributeKeys.FATAL_CAUSE).set(new Exception(fatalMessage));
            ctx.channel().close();
            logger.warn("Open Tunnel Error: {}", fatalMessage);
        }
        head.release();
    }


    /**
     * 处理数据透传消息
     */
    private void handleTransferMessage(final ChannelHandlerContext ctx, final ProtoMessage msg) throws Exception {
        logger.trace("handleTransferMessage: msg: {}", msg);
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();

        ctx.channel().attr(AttributeKeys.TUNNEL_TOKEN).set(tunnelToken);
        ctx.channel().attr(AttributeKeys.SESSION_TOKEN).set(sessionToken);
        final ProtoRequest protoRequest = ctx.channel().attr(AttributeKeys.PROTO_REQUEST).get();
        if (protoRequest != null) {
            final String localAddr = protoRequest.localAddr();
            final int localPort = protoRequest.localPort();
            localConnect.acquireLocalChannel(localAddr, localPort, tunnelToken, sessionToken, ctx.channel(),
                new LocalConnect.GetLocalContentChannelCallback() {
                    @Override
                    public void success(@NotNull Channel localChannel) {
                        localChannel.writeAndFlush(Unpooled.wrappedBuffer(msg.getData()));
                    }

                    @Override
                    public void error(Throwable cause) {
                    }
                }
            );
        }
    }

    /**
     * 处理连接本地隧道消息
     */
    private void handleRemoteConnectedMessage(final ChannelHandlerContext ctx, final ProtoMessage msg) throws Exception {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();

        ctx.channel().attr(AttributeKeys.TUNNEL_TOKEN).set(tunnelToken);
        ctx.channel().attr(AttributeKeys.SESSION_TOKEN).set(sessionToken);

        ProtoRequest protoRequest = ctx.channel().attr(AttributeKeys.PROTO_REQUEST).get();
        if (protoRequest != null) {
            final String localAddr = protoRequest.localAddr();
            final int localPort = protoRequest.localPort();
            localConnect.acquireLocalChannel(localAddr, localPort, tunnelToken, sessionToken, ctx.channel(),
                new LocalConnect.GetLocalContentChannelCallback() {
                    @Override
                    public void success(@NotNull Channel localChannel) {
                    }

                    @Override
                    public void error(Throwable cause) {
                    }
                }
            );
        }
    }

    /**
     * 处理用户隧道断开消息
     */
    private void handleRemoteDisconnectMessage(ChannelHandlerContext ctx, ProtoMessage msg) throws Exception {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();
        Channel localChannel = localConnect.removeLocalChannel(tunnelToken, sessionToken);
        if (localChannel != null) {
            localChannel.close();
        }
    }

    public interface ChannelListener {
        void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception;

        void tunnelConnected(@NotNull ChannelHandlerContext ctx);
    }

}
