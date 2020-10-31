package lighttunnel

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.Base64
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

object SslContextUtil {

    @JvmStatic
    @Throws(Exception::class)
    fun forBuiltinServer(): SslContext {
        val bytes = ByteBufUtil.getBytes(
            Base64.decode(
                Unpooled.wrappedBuffer(LightTunnelConfig.SERVER_JKS_BASE64.toByteArray())
            )
        )
        val keyStore = KeyStore.getInstance("JKS")
        ByteArrayInputStream(bytes).use {
            keyStore.load(it, LightTunnelConfig.SERVER_JKS_STORE_PASSWORD.toCharArray())
            val kmf = KeyManagerFactory.getInstance("SunX509")
            kmf.init(keyStore, LightTunnelConfig.SERVER_JKS_KEY_PASSWORD.toCharArray())
            return SslContextBuilder.forServer(kmf).build()
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun forBuiltinClient(): SslContext {
        val bytes = ByteBufUtil.getBytes(
            Base64.decode(
                Unpooled.wrappedBuffer(LightTunnelConfig.CLIENT_JKS_BASE64.toByteArray())
            )
        )
        val keyStore = KeyStore.getInstance("JKS")
        ByteArrayInputStream(bytes).use {
            keyStore.load(it, LightTunnelConfig.CLIENT_JKS_STORE_PASSWORD.toCharArray())
            val tmf = TrustManagerFactory.getInstance("SunX509")
            tmf.init(keyStore)
            return SslContextBuilder.forClient().trustManager(tmf).build()
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun forServer(jks: String, storePassword: String, keyPassword: String): SslContext {
        val keyStore = KeyStore.getInstance("JKS")
        FileInputStream(jks).use {
            keyStore.load(it, storePassword.toCharArray())
            val kmf = KeyManagerFactory.getInstance("SunX509")
            kmf.init(keyStore, keyPassword.toCharArray())
            return SslContextBuilder.forServer(kmf).build()
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun forClient(jks: String, storePassword: String): SslContext {
        val keyStore = KeyStore.getInstance("JKS")
        FileInputStream(jks).use {
            keyStore.load(it, storePassword.toCharArray())
            val tmf = TrustManagerFactory.getInstance("SunX509")
            tmf.init(keyStore)
            return SslContextBuilder.forClient().trustManager(tmf).build()
        }
    }

}
