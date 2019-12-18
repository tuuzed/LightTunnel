package lighttunnel.server

class LTSimpleRequestInterceptor(
    /** 预置Token */
    authToken: String? = null,
    /** 端口白名单 */
    allowPorts: String? = null
) : LTRequestInterceptor by LTRequestInterceptorImpl(authToken, allowPorts),
    LTHttpRequestInterceptor by LTHttpRequestInterceptorImpl()
