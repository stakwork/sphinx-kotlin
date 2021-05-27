package chat.sphinx.feature_crypto_rsa

import chat.sphinx.concept_crypto_rsa.*
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_rsa.*
import com.github.xiangyuecn.rsajava.RSA_PEM
import io.matthewnelson.crypto_common.annotations.UnencryptedDataAccess
import io.matthewnelson.crypto_common.clazzes.EncryptedString
import io.matthewnelson.crypto_common.clazzes.UnencryptedByteArray
import io.matthewnelson.crypto_common.clazzes.UnencryptedString
import io.matthewnelson.crypto_common.extensions.encodeToByteArray
import io.matthewnelson.crypto_common.extensions.isValidUTF8
import io.matthewnelson.crypto_common.extensions.toCharArray
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.base64.decodeBase64ToArray
import okio.base64.encodeBase64
import okio.base64.encodeBase64ToByteArray
import java.nio.ByteBuffer
import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import javax.crypto.Cipher

@Suppress("NOTHING_TO_INLINE")
inline fun RSA_PEM.clear(byte: Byte = '0'.code.toByte()) {
    Key_Modulus?.fill(byte)
    Key_Exponent?.fill(byte)
    Key_D?.fill(byte)
    Val_P?.fill(byte)
    Val_Q?.fill(byte)
    Val_DP?.fill(byte)
    Val_DQ?.fill(byte)
    Val_InverseQ?.fill(byte)
}

inline val RSA_PEM.blockSize: Int
    get() = Key_Modulus.size

inline val RSA_PEM.maxBytes: Int
    get() = blockSize - 11

@Suppress("SpellCheckingInspection")
open class RSAImpl(val algorithm: RSAAlgorithm): RSA() {

    override suspend fun generateKeyPair(
        keySize: KeySize,
        dispatcher: CoroutineDispatcher?,
        pkcsType: PKCSType,
    ): Response<RSAKeyPair, ResponseError> {
        try {
            val generator: KeyPairGenerator = KeyPairGenerator.getInstance(RSAAlgorithm.ALGORITHM_RSA)
            generator.initialize(keySize.value, SecureRandom())

            val keys: KeyPair = dispatcher?.let {
                withContext(it) {
                    generator.genKeyPair()
                }
            } ?: generator.genKeyPair()

            if (pkcsType is PKCSType.PKCS8) {
                return Response.Success(
                    RSAKeyPair(
                        RsaPrivateKey(keys.private.encoded.encodeBase64ToByteArray().toCharArray()),
                        RsaPublicKey(keys.public.encoded.encodeBase64ToByteArray().toCharArray())
                    )
                ).also {
                    keys.private.encoded?.fill('0'.code.toByte())
                }
            }

            val rsaPem = RSA_PEM(keys.public as RSAPublicKey, keys.private as RSAPrivateKey)

            return Response.Success(
                RSAKeyPair(
                    RsaPrivateKey(rsaPem.ToPEM_PKCS1_Bytes(false).toCharArray()),
                    RsaPublicKey(rsaPem.ToPEM_PKCS1_Bytes(true).toCharArray()),
                )
            ).also {
                keys.private.encoded?.fill('0'.code.toByte())
                rsaPem.clear()
            }

        } catch (e: Exception) {
            return Response.Error(ResponseError("RSA Key generation failure", e))
        }
    }

    override suspend fun decrypt(
        rsaPrivateKey: RsaPrivateKey,
        text: EncryptedString,
        dispatcher: CoroutineDispatcher,
    ): Response<UnencryptedByteArray, ResponseError> {
        if (text.value.isEmpty()) {
            return Response.Error(
                ResponseError("EncryptedString was empty")
            )
        }

        val dataBytes: ByteArray = text.value.decodeBase64ToArray()
            ?: return Response.Error(
                ResponseError("EncryptedString was not base64 encoded")
            )

        return try {
            val decrypted: ByteArray = withContext(dispatcher) {

                val rsaPem: RSA_PEM = RSA_PEM.FromPEM(rsaPrivateKey.value, true)
                val blockSize: Int = rsaPem.blockSize

                if (dataBytes.size > blockSize) {
                    val privKey: RSAPrivateKey = rsaPem.rsaPrivateKey

                    val arrSize: Int = (dataBytes.size / blockSize)

                    @Suppress("RemoveExplicitTypeArguments")
                    val arr = Array<ByteArray>(arrSize) { index ->
                        val fromIndex: Int = (index * blockSize)
                        val toIndex: Int = if ((fromIndex + blockSize) <= dataBytes.size) {
                            fromIndex + blockSize
                        } else {
                            dataBytes.size
                        }

                        dataBytes.copyOfRange(fromIndex, toIndex)
                    }

                    val buffer: ByteBuffer = ByteBuffer.wrap(
                        ByteArray(blockSize * arrSize)
                    )

                    var finalSize = 0
                    try {

                        for (ba in arr) {
                            val cipher: Cipher = Cipher.getInstance(algorithm.value)
                            cipher.init(Cipher.DECRYPT_MODE, privKey)
                            cipher.doFinal(ba).let { bytes ->
                                buffer.put(
                                    ByteBuffer.wrap(bytes).array().also {
                                        finalSize += it.size
                                    }
                                )
                            }
                        }

                    } finally {

                        for (ba in arr) {
                            ba.fill('0'.code.toByte())
                        }
                        rsaPem.clear()

                    }

                    buffer.array().let { decrypted ->
                        decrypted.copyOfRange(fromIndex = 0, toIndex = finalSize).also {
                            decrypted.fill('0'.code.toByte())
                        }
                    }

                } else {

                    try {
                        val cipher: Cipher = Cipher.getInstance(algorithm.value)
                        cipher.init(Cipher.DECRYPT_MODE, rsaPem.rsaPrivateKey)
                        cipher.doFinal(dataBytes)
                    } finally {
                        rsaPem.clear()
                    }

                }

            }

            if (!decrypted.isValidUTF8) {
                throw CharacterCodingException()
            }

            Response.Success(UnencryptedByteArray(decrypted))
        } catch (e: CharacterCodingException) {
            Response.Error(
                ResponseError(
                    """
                        Decryption failed.
                        Decrypted value produced invalid UTF-8 encoded bytes.
                        Current Algorithm: ${algorithm.value}
                    """.trimIndent(),
                    e
                )
            )
        } catch (e: Exception) {
            Response.Error(ResponseError("Decryption failed", e))
        }
    }

    @OptIn(UnencryptedDataAccess::class)
    override suspend fun encrypt(
        rsaPublicKey: RsaPublicKey,
        text: UnencryptedString,
        formatOutput: Boolean,
        dispatcher: CoroutineDispatcher
    ): Response<EncryptedString, ResponseError> {
        if (text.value.isEmpty()) {
            return Response.Error(
                ResponseError("UnencryptedString was empty")
            )
        }

        return try {
            val encrypted: ByteArray = withContext(dispatcher) {

                val rsaPem: RSA_PEM = RSA_PEM.FromPEM(rsaPublicKey.value, false)
                val dataBytes: ByteArray = text.value.encodeToByteArray()
                val maxBytes: Int = rsaPem.maxBytes

                if (dataBytes.size > maxBytes) {
                    val pubKey: RSAPublicKey = rsaPem.rsaPublicKey

                    val arrSize = (dataBytes.size / maxBytes) + 1

                    @Suppress("RemoveExplicitTypeArguments")
                    val arr = Array<ByteArray>(arrSize) { index ->
                        val fromIndex: Int = (index * maxBytes)
                        val toIndex: Int = if ( (fromIndex + maxBytes) <= dataBytes.size) {
                            fromIndex + maxBytes
                        } else {
                            dataBytes.size
                        }

                        dataBytes.copyOfRange(fromIndex, toIndex)
                    }

                    val buffer = ByteBuffer.wrap(ByteArray(rsaPem.blockSize * arrSize))

                    try {
                        for (ba in arr) {
                            val cipher: Cipher = Cipher.getInstance(algorithm.value)
                            cipher.init(Cipher.ENCRYPT_MODE, pubKey)
                            cipher.doFinal(ba).let { bytes ->
                                buffer.put(
                                    ByteBuffer.wrap(bytes).array()
                                )
                            }
                        }
                    } finally {
                        rsaPem.clear()
                    }

                    buffer.array()
                } else {
                    try {
                        val cipher: Cipher = Cipher.getInstance(algorithm.value)
                        cipher.init(Cipher.ENCRYPT_MODE, rsaPem.rsaPublicKey)
                        cipher.doFinal(dataBytes)
                    } finally {
                        rsaPem.clear()
                    }
                }

            }

            val string: String = if (formatOutput) {
                encrypted.encodeBase64().replace("(.{64})".toRegex(), "$1\n")
            } else {
                encrypted.encodeBase64()
            }

            Response.Success(EncryptedString(string))
        } catch (e: Exception) {
            Response.Error(ResponseError("Encryption failed", e))
        }
    }

    override suspend fun sign(
        rsaPrivateKey: RsaPrivateKey,
        text: String,
        algorithm: SignatureAlgorithm,
        dispatcher: CoroutineDispatcher
    ): Response<RsaSignedString, ResponseError> {
        if (text.isEmpty()) {
            return Response.Error(
                ResponseError("String value to sign was empty")
            )
        }

        return try {
            val signed: ByteArray = withContext(dispatcher) {

                val rsaPem: RSA_PEM = RSA_PEM.FromPEM(rsaPrivateKey.value, true)

                try {
                    val signature: Signature = Signature.getInstance(algorithm.value)
                    signature.initSign(rsaPem.rsaPrivateKey)
                    signature.update(text.encodeToByteArray())
                    signature.sign()
                } finally {
                    rsaPem.clear()
                }
            }

            Response.Success(
                RsaSignedString(
                    text,
                    RsaSignature(signed),
                )
            )
        } catch (e: Exception) {
            Response.Error(ResponseError("Signing failed", e))
        }
    }

    override suspend fun verifySignature(
        rsaPublicKey: RsaPublicKey,
        signedString: RsaSignedString,
        algorithm: SignatureAlgorithm,
        dispatcher: CoroutineDispatcher
    ): Response<Boolean, ResponseError> {
        if (signedString.signature.value.isEmpty()) {
            return Response.Error(
                ResponseError("RsaSignature was empty")
            )
        }

        if (signedString.text.isEmpty()) {
            return Response.Error(
                ResponseError("String value to verify was empty")
            )
        }

        return try {
            val verification: Boolean = withContext(dispatcher) {

                val rsaPem: RSA_PEM = RSA_PEM.FromPEM(rsaPublicKey.value, false)

                try {
                    val signVerify: Signature = Signature.getInstance(algorithm.value)
                    signVerify.initVerify(rsaPem.rsaPublicKey)
                    signVerify.update(signedString.text.encodeToByteArray())
                    signVerify.verify(signedString.signature.value)
                } finally {
                    rsaPem.clear()
                }
            }

            Response.Success(verification)
        } catch (e: Exception) {
            Response.Error(ResponseError("Signature Verification failed", e))
        }
    }
}
