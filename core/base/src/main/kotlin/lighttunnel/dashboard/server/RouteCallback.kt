package lighttunnel.dashboard.server

import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse

typealias RouteCallback = (request: FullHttpRequest) -> FullHttpResponse