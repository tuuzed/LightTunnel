package krp.krp.extra

import krp.common.exception.KrpException

class ChannelInactiveExtra(
    val isForceOff: Boolean,
    val cause: KrpException
)
