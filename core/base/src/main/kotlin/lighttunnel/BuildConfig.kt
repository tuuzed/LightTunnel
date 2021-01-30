@file:Suppress("HasPlatformType")

package lighttunnel

import lighttunnel.internal._BuildConfig
import lighttunnel.internal.base.utils.Base64FileUtils

object BuildConfig {
    const val PROTO_VERSION = lighttunnel.internal.base.proto.PROTO_VERSION
    const val VERSION_CODE = _BuildConfig.VERSION_CODE
    const val VERSION_NAME = _BuildConfig.VERSION_NAME
    const val LAST_COMMIT_SHA = _BuildConfig.LAST_COMMIT_SHA
    const val LAST_COMMIT_DATE = _BuildConfig.LAST_COMMIT_DATE
    const val BUILD_DATA = _BuildConfig.BUILD_DATA
    val SERVER_JKS_BYTES = Base64FileUtils.decode(_BuildConfig.SERVER_JKS_BASE64)
    const val SERVER_JKS_STORE_PASSWORD = _BuildConfig.SERVER_JKS_STORE_PASSWORD
    const val SERVER_JKS_KEY_PASSWORD = _BuildConfig.SERVER_JKS_KEY_PASSWORD
    val CLIENT_JKS_BYTES = Base64FileUtils.decode(_BuildConfig.CLIENT_JKS_BASE64)
    const val CLIENT_JKS_STORE_PASSWORD = _BuildConfig.CLIENT_JKS_STORE_PASSWORD
}
