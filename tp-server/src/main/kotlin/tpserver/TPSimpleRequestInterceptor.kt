package tpserver

class TPSimpleRequestInterceptor(
    /** 预置Token */
    authToken: String? = null,
    /** 端口白名单 */
    allowPorts: String? = null
) : TPRequestInterceptor by TPRequestInterceptorImpl(authToken, allowPorts),
    TPHttpRequestInterceptor by TPHttpRequestInterceptorImpl()
