package lighttunnel.dashboard.client

import io.netty.handler.codec.http.FullHttpResponse

typealias RequestCallback = (response: FullHttpResponse) -> Unit
