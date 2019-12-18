package lighttunnel.proto

class LTException @JvmOverloads constructor(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

