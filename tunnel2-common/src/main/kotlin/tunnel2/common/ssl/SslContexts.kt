package tunnel2.common.ssl

import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

object SslContexts {

    @JvmStatic
    @Throws(Exception::class)
    fun forServer(
        jks: String,
        storepass: String,
        keypass: String
    ): SslContext {
        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(FileInputStream(jks), storepass.toCharArray())
        val kmf = KeyManagerFactory.getInstance("SunX509")
        kmf.init(keyStore, keypass.toCharArray())
        return SslContextBuilder.forServer(kmf).build()
    }

    @JvmStatic
    @Throws(Exception::class)
    fun forClient(
        jks: String,
        storepass: String
    ): SslContext {
        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(FileInputStream(jks), storepass.toCharArray())
        val tmf = TrustManagerFactory.getInstance("SunX509")
        tmf.init(keyStore)
        return SslContextBuilder.forClient().trustManager(tmf).build()
    }
}
