package tpcommon

class TPException @JvmOverloads constructor(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

