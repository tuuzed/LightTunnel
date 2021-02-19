@file:Suppress("HasPlatformType")

package lighttunnel.base

import lighttunnel.base.gen.GenBuildConfig
import lighttunnel.base.utils.Base64Files

object BuildConfig {
    const val PROTO_VERSION = lighttunnel.base.proto.PROTO_VERSION
    const val VERSION_CODE = GenBuildConfig.VERSION_CODE
    const val VERSION_NAME = GenBuildConfig.VERSION_NAME
    const val LAST_COMMIT_SHA = GenBuildConfig.LAST_COMMIT_SHA
    const val LAST_COMMIT_DATE = GenBuildConfig.LAST_COMMIT_DATE
    const val BUILD_DATA = GenBuildConfig.BUILD_DATA
    val SERVER_JKS_BYTES = Base64Files.decode(GenBuildConfig.SERVER_JKS_BASE64)
    const val SERVER_JKS_STORE_PASSWORD = GenBuildConfig.SERVER_JKS_STORE_PASSWORD
    const val SERVER_JKS_KEY_PASSWORD = GenBuildConfig.SERVER_JKS_KEY_PASSWORD
    val CLIENT_JKS_BYTES = Base64Files.decode(GenBuildConfig.CLIENT_JKS_BASE64)
    const val CLIENT_JKS_STORE_PASSWORD = GenBuildConfig.CLIENT_JKS_STORE_PASSWORD
}
