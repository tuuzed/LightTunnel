package com.tuuzed.tunnel.common.protocol;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * 心跳处理器
 */
public class TunnelHeartbeatHandler extends IdleStateHandler {

    private static Logger logger = LoggerFactory.getLogger(TunnelHeartbeatHandler.class);

    public TunnelHeartbeatHandler() {
        this(90, 60, 0, TimeUnit.SECONDS);
    }

    public TunnelHeartbeatHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }

    public TunnelHeartbeatHandler(long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit) {
        super(readerIdleTime, writerIdleTime, allIdleTime, unit);
    }

    public TunnelHeartbeatHandler(boolean observeOutput, long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit) {
        super(observeOutput, readerIdleTime, writerIdleTime, allIdleTime, unit);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT == evt) {
            logger.debug("channel write timeout {}", ctx);
            ctx.channel().writeAndFlush(TunnelMessage.newInstance(TunnelMessage.MESSAGE_TYPE_HEARTBEAT_PING));
        } else if (IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT == evt) {
            logger.warn("channel read timeout {}", ctx);
            ctx.channel().close();
        }
        super.channelIdle(ctx, evt);
    }
}
