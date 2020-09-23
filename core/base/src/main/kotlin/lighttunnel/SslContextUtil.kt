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

    private const val BASE64_SERVER_JKS = "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"
    private const val BASE64_CLIENT_JKS = "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"

    @JvmStatic
    @Throws(Exception::class)
    fun forBuiltinServer(): SslContext {
        val bytes = ByteBufUtil.getBytes(
            Base64.decode(
                Unpooled.wrappedBuffer(BASE64_SERVER_JKS.toByteArray())
            )
        )
        val keyStore = KeyStore.getInstance("JKS")
        ByteArrayInputStream(bytes).use {
            keyStore.load(it, "ltspass".toCharArray())
            val kmf = KeyManagerFactory.getInstance("SunX509")
            kmf.init(keyStore, "ltspass".toCharArray())
            return SslContextBuilder.forServer(kmf).build()
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun forBuiltinClient(): SslContext {
        val bytes = ByteBufUtil.getBytes(
            Base64.decode(
                Unpooled.wrappedBuffer(BASE64_CLIENT_JKS.toByteArray())
            )
        )
        val keyStore = KeyStore.getInstance("JKS")
        ByteArrayInputStream(bytes).use {
            keyStore.load(it, "ltcpass".toCharArray())
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
