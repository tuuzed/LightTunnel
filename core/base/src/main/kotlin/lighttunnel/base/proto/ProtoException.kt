package lighttunnel.base.proto

class ProtoException constructor(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

