package lighttunnel

class ProtoException constructor(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

