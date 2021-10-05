package lighttunnel.base

import lighttunnel.base.gen.GenBuildConfig
import lighttunnel.base.utils.base64Decode

object BuildConfig {
    const val PROTO_VERSION = lighttunnel.base.proto.PROTO_VERSION
    const val VERSION_CODE = GenBuildConfig.VERSION_CODE
    const val VERSION_NAME = GenBuildConfig.VERSION_NAME
    const val LAST_COMMIT_SHA = GenBuildConfig.LAST_COMMIT_SHA
    const val LAST_COMMIT_DATE = GenBuildConfig.LAST_COMMIT_DATE
    const val BUILD_DATA = GenBuildConfig.BUILD_DATA
    val BUILTIN_SERVER_JKS_BYTES = base64Decode(GenBuildConfig.BUILTIN_SERVER_JKS_BASE64)
    const val BUILTIN_SERVER_JKS_STORE_PASSWORD = GenBuildConfig.BUILTIN_SERVER_JKS_STORE_PASSWORD
    const val BUILTIN_SERVER_JKS_KEY_PASSWORD = GenBuildConfig.BUILTIN_SERVER_JKS_KEY_PASSWORD
    val BUILTIN_CLIENT_JKS_BYTES = base64Decode(GenBuildConfig.BUILTIN_CLIENT_JKS_BASE64)
    const val BUILTIN_CLIENT_JKS_STORE_PASSWORD = GenBuildConfig.BUILTIN_CLIENT_JKS_STORE_PASSWORD
}
