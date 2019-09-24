package tpserver

import tpcommon.TPException
import tpcommon.TPRequest

interface TPRequestInterceptor {

    companion object {
        val EMPTY_IMPL = object : TPRequestInterceptor {
            override fun handleTPRequest(request: TPRequest): TPRequest = request
        }
    }

    @Throws(TPException::class)
    fun handleTPRequest(request: TPRequest): TPRequest

}
