package com.healthmetrix.connector.commons.crypto

import com.healthmetrix.connector.commons.decodeBase64
import com.healthmetrix.connector.commons.logger
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringWriter
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.Security
import java.security.spec.InvalidKeySpecException
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class RsaPrivateKey(private val key: PrivateKey) {

    fun decrypt(input: ByteArray): ByteArray? = try {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        val oaepParams = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT)

        val cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA-256AndMGF1Padding", "BC").apply {
            init(Cipher.DECRYPT_MODE, key, oaepParams)
        }

        cipher.doFinal(input)
    } catch (ex: Exception) {
        logger.warn("Error occurred RSA decrypting ByteArray", ex)
        null
    }

    companion object {
        fun buildFromB64String(keyB64: String): RsaPrivateKey? {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(BouncyCastleProvider())
            }

            val keySpec = keyB64.decodeBase64()?.inputStream()?.reader()
                .let(::PemReader)
                .readPemObject()
                .content
                .let(::PKCS8EncodedKeySpec)

            return try {
                val kf = KeyFactory.getInstance("RSA")
                RsaPrivateKey(kf.generatePrivate(keySpec))
            } catch (ex: InvalidKeySpecException) {
                logger.warn("Input string is not valid for PKCS8EncodedKeySpec", ex)
                null
            } catch (ex: ClassCastException) {
                logger.warn("Error casting PrivateKey to RSAPrivateKey", ex)
                null
            } catch (ex: NoSuchAlgorithmException) {
                logger.warn("Error finding RSA KeyFactory on listed providers", ex)
                null
            }
        }

        fun toPemKeyString(key: ByteArray): String {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(BouncyCastleProvider())
            }

            return with(StringWriter()) {
                with(PemWriter(this)) {
                    writeObject(PemObject("RSA PRIVATE KEY", key))
                    close()
                }

                buffer.toString()
            }
        }
    }
}
