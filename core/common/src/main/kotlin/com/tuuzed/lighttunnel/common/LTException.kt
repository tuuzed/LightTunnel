package com.tuuzed.lighttunnel.common

class LTException @JvmOverloads constructor(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

