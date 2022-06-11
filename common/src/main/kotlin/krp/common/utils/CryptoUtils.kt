package krp.common.utils

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random


object CryptoUtils {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val random by lazy { Random(System.nanoTime()) }

    fun randomAES128Key(): ByteArray {
        val sb = StringBuilder()
        for (i in 0 until 16) {
            sb.append(ALPHABET[randomInt(0, ALPHABET.length)])
        }
        return sb.toString().toByteArray()
    }

    fun randomRSAKeyPair(): Pair<ByteArray, ByteArray> {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.genKeyPair()
        return kp.private.encoded to kp.public.encoded
    }

    @Throws(Exception::class)
    fun encryptAES128(data: ByteArray, key128: ByteArray): ByteArray = templateAES128(data, key128, true)

    @Throws(Exception::class)
    fun decryptAES128(data: ByteArray, key128: ByteArray): ByteArray = templateAES128(data, key128, false)

    @Throws(Exception::class)
    fun encryptRSA(data: ByteArray, pub: ByteArray): ByteArray = templateRSA(data, pub, true)

    @Throws(Exception::class)
    fun decryptRSA(data: ByteArray, pri: ByteArray): ByteArray = templateRSA(data, pri, false)

    private fun randomInt(from: Int, until: Int): Int = random.nextInt(from, until)

    @Throws(Exception::class)
    private fun templateRSA(data: ByteArray, pubOrPri: ByteArray, encrypt: Boolean): ByteArray {
        val keyFactory = KeyFactory.getInstance("RSA")
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        val keySpec = if (encrypt) X509EncodedKeySpec(pubOrPri) else PKCS8EncodedKeySpec(pubOrPri)
        val key = if (encrypt) keyFactory.generatePublic(keySpec) else keyFactory.generatePrivate(keySpec)
        cipher.init(if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    @Throws(Exception::class)
    private fun templateAES128(data: ByteArray, key128: ByteArray, encrypt: Boolean): ByteArray {
        val key = SecretKeySpec(key128, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = IvParameterSpec("0123456789ABCDEF".toByteArray())
        cipher.init(if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, key, iv)
        return cipher.doFinal(data)
    }


}
