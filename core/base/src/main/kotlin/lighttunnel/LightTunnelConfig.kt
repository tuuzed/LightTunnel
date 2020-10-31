package lighttunnel

object LightTunnelConfig {
    const val PROTO_VERSION = lighttunnel.internal.base.proto.PROTO_VERSION
    const val VERSION_CODE = BuildConfig.VERSION_CODE
    const val VERSION_NAME = BuildConfig.VERSION_NAME
    const val LAST_COMMIT_SHA = BuildConfig.LAST_COMMIT_SHA
    const val LAST_COMMIT_DATE = BuildConfig.LAST_COMMIT_DATE
    const val BUILD_DATA = BuildConfig.BUILD_DATA
    const val SERVER_JKS_BASE64 = BuildConfig.SERVER_JKS_BASE64
    const val SERVER_JKS_STORE_PASSWORD = BuildConfig.SERVER_JKS_STORE_PASSWORD
    const val SERVER_JKS_KEY_PASSWORD = BuildConfig.SERVER_JKS_KEY_PASSWORD
    const val CLIENT_JKS_BASE64 = BuildConfig.CLIENT_JKS_BASE64
    const val CLIENT_JKS_STORE_PASSWORD = BuildConfig.CLIENT_JKS_STORE_PASSWORD
}