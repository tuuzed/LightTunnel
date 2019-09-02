package tunnel2.common

class TunnelException @JvmOverloads constructor(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

