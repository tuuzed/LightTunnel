package lighttunnel.client.extra

import lighttunnel.common.exception.LightTunnelException

class ChannelInactiveExtra(
    val isForceOff: Boolean,
    val cause: LightTunnelException
)
