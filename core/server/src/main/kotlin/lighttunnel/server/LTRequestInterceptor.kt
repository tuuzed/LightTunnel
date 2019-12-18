package lighttunnel.server

import lighttunnel.proto.LTException
import lighttunnel.proto.LTRequest

interface LTRequestInterceptor {

    companion object {
        val EMPTY_IMPL = object : LTRequestInterceptor {
            override fun handleTPRequest(request: LTRequest): LTRequest = request
        }
    }

    @Throws(LTException::class)
    fun handleTPRequest(request: LTRequest): LTRequest

}
