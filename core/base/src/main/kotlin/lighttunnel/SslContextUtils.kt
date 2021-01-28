package lighttunnel

import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import lighttunnel.internal.base.utils.Base64Utils
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

object SslContextUtils {

    @JvmStatic
    @Throws(Exception::class)
    fun forBuiltinServer(): SslContext {
        val bytes = Base64Utils.decode(BuildConfig.SERVER_JKS_BASE64)
        val keyStore = KeyStore.getInstance("JKS")
        return ByteArrayInputStream(bytes).use {
            keyStore.load(it, BuildConfig.SERVER_JKS_STORE_PASSWORD.toCharArray())
            val kmf = KeyManagerFactory.getInstance("SunX509")
            kmf.init(keyStore, BuildConfig.SERVER_JKS_KEY_PASSWORD.toCharArray())
            SslContextBuilder.forServer(kmf).build()
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun forBuiltinClient(): SslContext {
        val bytes = Base64Utils.decode(BuildConfig.CLIENT_JKS_BASE64)
        val keyStore = KeyStore.getInstance("JKS")
        return ByteArrayInputStream(bytes).use {
            keyStore.load(it, BuildConfig.CLIENT_JKS_STORE_PASSWORD.toCharArray())
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
