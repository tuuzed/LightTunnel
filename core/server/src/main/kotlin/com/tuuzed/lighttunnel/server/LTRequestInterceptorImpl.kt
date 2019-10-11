package com.tuuzed.lighttunnel.server

import com.tuuzed.lighttunnel.common.LTException
import com.tuuzed.lighttunnel.common.LTRequest
import com.tuuzed.lighttunnel.common.LTType

class LTRequestInterceptorImpl(
    /** 预置Token */
    private val authToken: String?,
    /** 端口白名单 */
    private val allowPorts: String?
) : LTRequestInterceptor {

    @Throws(LTException::class)
    override fun handleTPRequest(request: LTRequest): LTRequest {
        if (authToken != null && authToken != request.authToken) {
            throw LTException("request($request), Bad Auth Token(${request.authToken})")
        }
        return when (request.type) {
            LTType.TCP -> {
                val remotePort = request.remotePort
                if (allowPorts != null && !hasInPortRange(allowPorts, remotePort)) {
                    throw LTException("request($request), remotePort($remotePort) Not allowed to use.")
                }
                request
            }
            else -> request
        }
    }
}
