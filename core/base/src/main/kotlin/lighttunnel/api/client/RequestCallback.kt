package lighttunnel.api.client

import io.netty.handler.codec.http.FullHttpResponse

typealias RequestCallback = (response: FullHttpResponse) -> Unit
