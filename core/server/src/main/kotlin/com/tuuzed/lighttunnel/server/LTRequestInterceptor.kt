package com.tuuzed.lighttunnel.server

import com.tuuzed.lighttunnel.common.LTException
import com.tuuzed.lighttunnel.common.LTRequest

interface LTRequestInterceptor {

    companion object {
        val EMPTY_IMPL = object : LTRequestInterceptor {
            override fun handleTPRequest(request: LTRequest): LTRequest = request
        }
    }

    @Throws(LTException::class)
    fun handleTPRequest(request: LTRequest): LTRequest

}
