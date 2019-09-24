package tpserver

import tpcommon.TPException
import tpcommon.TPRequest
import tpcommon.TPType

class TPRequestInterceptorImpl(
    /** 预置Token */
    private val authToken: String?,
    /** 端口白名单 */
    private val allowPorts: String?
) : TPRequestInterceptor {

    @Throws(TPException::class)
    override fun handleTPRequest(request: TPRequest): TPRequest {
        if (authToken != null && authToken != request.authToken) {
            throw TPException("request($request), Bad Auth Token(${request.authToken})")
        }
        return when (request.type) {
            TPType.TCP -> {
                val remotePort = request.remotePort
                if (allowPorts != null && !hasInPortRange(allowPorts, remotePort)) {
                    throw TPException("request($request), remotePort($remotePort) Not allowed to use.")
                }
                request
            }
            else -> request
        }
    }
}
