package ltcmd.client

import lighttunnel.client.TunnelClient
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.RemoteInfo

class OnRemoteConnectListenerImpl : TunnelClient.OnRemoteConnectListener {

    private val logger by loggerDelegate()

    override fun onRemoteConnected(remoteInfo: RemoteInfo?) {
        logger.info("onRemoteConnected: {}", remoteInfo)
    }

    override fun onRemoteDisconnect(remoteInfo: RemoteInfo?) {
        logger.info("onRemoteDisconnect: {}", remoteInfo)
    }

}