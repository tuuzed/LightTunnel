@file:Suppress("unused")

package lighttunnel.common.utils

import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import lighttunnel.common.utils.HexStringUtils.hexStringToBytes
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

object SslContextUtils {

    @Throws(Exception::class)
    fun forBuiltinServer(): SslContext = forServer(
        ByteArrayInputStream(hexStringToBytes(ManifestUtils.jksServerHex)),
        ManifestUtils.jksServerStore,
        ManifestUtils.jksServerKey,
    )

    @Throws(Exception::class)
    fun forBuiltinClient(): SslContext =
        forClient(ByteArrayInputStream(hexStringToBytes(ManifestUtils.jksClientHex)), ManifestUtils.jksClientStore)

    @Throws(Exception::class)
    fun forServer(jks: String, storePassword: String, keyPassword: String): SslContext = forServer(
        FileInputStream(jks),
        storePassword,
        keyPassword,
    )

    @Throws(Exception::class)
    fun forClient(jks: String, storePassword: String): SslContext = forClient(FileInputStream(jks), storePassword)

    @Throws(Exception::class)
    private fun forServer(jks: InputStream, storePassword: String, keyPassword: String): SslContext {
        val keyStore = KeyStore.getInstance("JKS")
        return jks.use {
            keyStore.load(it, storePassword.toCharArray())
            val kmf = KeyManagerFactory.getInstance("SunX509")
            kmf.init(keyStore, keyPassword.toCharArray())
            SslContextBuilder.forServer(kmf).build()
        }
    }

    @Throws(Exception::class)
    private fun forClient(jks: InputStream, storePassword: String): SslContext {
        val keyStore = KeyStore.getInstance("JKS")
        return jks.use {
            keyStore.load(it, storePassword.toCharArray())
            val tmf = TrustManagerFactory.getInstance("SunX509")
            tmf.init(keyStore)
            SslContextBuilder.forClient().trustManager(tmf).build()
        }
    }

}
