package lighttunnel.util

import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

object SslContextUtil {

    @JvmStatic
    @Throws(Exception::class)
    fun forServer(jks: String, storePassword: String, keyPassword: String): SslContext {
        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(FileInputStream(jks), storePassword.toCharArray())
        val kmf = KeyManagerFactory.getInstance("SunX509")
        kmf.init(keyStore, keyPassword.toCharArray())
        return SslContextBuilder.forServer(kmf).build()
    }

    @JvmStatic
    @Throws(Exception::class)
    fun forClient(jks: String, storePassword: String): SslContext {
        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(FileInputStream(jks), storePassword.toCharArray())
        val tmf = TrustManagerFactory.getInstance("SunX509")
        tmf.init(keyStore)
        return SslContextBuilder.forClient().trustManager(tmf).build()
    }
}
