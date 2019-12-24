package lighttunnel.server.interceptor

class SimpleRequestInterceptor(
    /** 预置Token */
    authToken: String? = null,
    /** 端口白名单 */
    allowPorts: String? = null
) : TunnelRequestInterceptor by TunnelRequestInterceptorImpl(authToken, allowPorts),
    HttpRequestInterceptor by HttpRequestInterceptorImpl() {

    companion object {
        val defaultImpl by lazy { SimpleRequestInterceptor() }
    }
}
