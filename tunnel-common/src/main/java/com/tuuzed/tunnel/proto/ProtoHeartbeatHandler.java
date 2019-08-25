package com.tuuzed.tunnel.proto;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 心跳处理器
 */
public class ProtoHeartbeatHandler extends IdleStateHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProtoHeartbeatHandler.class);

    public ProtoHeartbeatHandler() {
        this(60 * 5, 60 * 3, 0, TimeUnit.SECONDS);
    }

    public ProtoHeartbeatHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }

    public ProtoHeartbeatHandler(long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit) {
        super(readerIdleTime, writerIdleTime, allIdleTime, unit);
    }

    public ProtoHeartbeatHandler(boolean observeOutput, long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit) {
        super(observeOutput, readerIdleTime, writerIdleTime, allIdleTime, unit);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT == evt) {
            logger.trace("channel write timeout {}", ctx);
            ctx.channel().writeAndFlush(new ProtoMessage(ProtoMessage.Type.HEARTBEAT_PING, null, null));
        } else if (IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT == evt) {
            logger.trace("channel read timeout {}", ctx);
            ctx.channel().close();
        }
        super.channelIdle(ctx, evt);
    }
}
