package com.healthmetrix.connector.commons.crypto

import com.healthmetrix.connector.commons.decodeBase64
import com.healthmetrix.connector.commons.logger
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemReader
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.Security
import java.security.interfaces.RSAPublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class RsaPublicKey(private val key: RSAPublicKey) {

    fun encrypt(input: ByteArray): ByteArray? = try {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        val oaepParams = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT)

        val cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA-256AndMGF1Padding", "BC").apply {
            init(Cipher.ENCRYPT_MODE, key, oaepParams)
        }

        cipher.doFinal(input)
    } catch (ex: Exception) {
        logger.warn("Error occurred RSA decrypting ByteArray", ex)
        null
    }

    companion object {
        fun buildFromB64String(keyB64: String): RSAPublicKey? {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(BouncyCastleProvider())
            }

            val keySpec = keyB64.decodeBase64()?.inputStream()?.reader()
                .let(::PemReader)
                .readPemObject()
                .content
                .let(::X509EncodedKeySpec)

            return try {
                val kf = KeyFactory.getInstance("RSA")
                kf.generatePublic(keySpec) as RSAPublicKey
            } catch (ex: InvalidKeySpecException) {
                logger.warn("Input string is not valid for X509EncodedKeySpec", ex)
                null
            } catch (ex: ClassCastException) {
                logger.warn("Error casting PublicKey to RSAPublicKey", ex)
                null
            } catch (ex: NoSuchAlgorithmException) {
                logger.warn("Error finding RSA KeyFactory on listed providers", ex)
                null
            }
        }
    }
}
