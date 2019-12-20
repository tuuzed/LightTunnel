package lighttunnel.server.interceptor

import lighttunnel.server.http.HttpRequestInterceptor
import lighttunnel.server.http.HttpRequestInterceptorImpl

class SimpleRequestInterceptor(
    /** 预置Token */
    authToken: String? = null,
    /** 端口白名单 */
    allowPorts: String? = null
) : RequestInterceptor by RequestInterceptorImpl(authToken, allowPorts),
    HttpRequestInterceptor by HttpRequestInterceptorImpl()
