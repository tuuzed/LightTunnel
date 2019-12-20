package lighttunnel.server.interceptor

import lighttunnel.proto.ProtoException
import lighttunnel.proto.ProtoRequest

interface RequestInterceptor {

    companion object {
        val EMPTY_IMPL = object : RequestInterceptor {
            override fun handleTPRequest(request: ProtoRequest): ProtoRequest = request
        }
    }

    @Throws(ProtoException::class)
    fun handleTPRequest(request: ProtoRequest): ProtoRequest

}
