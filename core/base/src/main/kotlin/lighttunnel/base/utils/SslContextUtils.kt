package lighttunnel.base.utils

import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import lighttunnel.base.BuildConfig
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

object SslContextUtils {

    @JvmStatic
    @Throws(Exception::class)
    fun forBuiltinServer(): SslContext {
        val keyStore = KeyStore.getInstance("JKS")
        return ByteArrayInputStream(BuildConfig.BUILTIN_SERVER_JKS_BYTES).use {
            keyStore.load(it, BuildConfig.BUILTIN_SERVER_JKS_STORE_PASSWORD.toCharArray())
            val kmf = KeyManagerFactory.getInstance("SunX509")
            kmf.init(keyStore, BuildConfig.BUILTIN_SERVER_JKS_KEY_PASSWORD.toCharArray())
            SslContextBuilder.forServer(kmf).build()
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun forBuiltinClient(): SslContext {
        val keyStore = KeyStore.getInstance("JKS")
        return ByteArrayInputStream(BuildConfig.BUILTIN_CLIENT_JKS_BYTES).use {
            keyStore.load(it, BuildConfig.BUILTIN_CLIENT_JKS_STORE_PASSWORD.toCharArray())
            val tmf = TrustManagerFactory.getInstance("SunX509")
            tmf.init(keyStore)
            SslContextBuilder.forClient().trustManager(tmf).build()
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun forServer(jks: String, storePassword: String, keyPassword: String): SslContext {
        val keyStore = KeyStore.getInstance("JKS")
        return FileInputStream(jks).use {
            keyStore.load(it, storePassword.toCharArray())
            val kmf = KeyManagerFactory.getInstance("SunX509")
            kmf.init(keyStore, keyPassword.toCharArray())
            SslContextBuilder.forServer(kmf).build()
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun forClient(jks: String, storePassword: String): SslContext {
        val keyStore = KeyStore.getInstance("JKS")
        return FileInputStream(jks).use {
            keyStore.load(it, storePassword.toCharArray())
            val tmf = TrustManagerFactory.getInstance("SunX509")
            tmf.init(keyStore)
            SslContextBuilder.forClient().trustManager(tmf).build()
        }
    }

}
