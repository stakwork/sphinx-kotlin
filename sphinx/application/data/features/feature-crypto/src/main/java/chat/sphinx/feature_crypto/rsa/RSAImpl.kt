package chat.sphinx.feature_crypto.rsa

import chat.sphinx.concept_crypto.rsa.*
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.kotlin_response.ResponseError
import com.github.xiangyuecn.rsajava.RSA_PEM
import io.matthewnelson.k_openssl_common.annotations.RawPasswordAccess
import io.matthewnelson.k_openssl_common.annotations.UnencryptedDataAccess
import io.matthewnelson.k_openssl_common.clazzes.EncryptedString
import io.matthewnelson.k_openssl_common.clazzes.UnencryptedByteArray
import io.matthewnelson.k_openssl_common.clazzes.UnencryptedString
import io.matthewnelson.k_openssl_common.extensions.encodeToByteArray
import io.matthewnelson.k_openssl_common.extensions.toCharArray
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
inline fun RSA_PEM.clear(byte: Byte = '0'.toByte()) {
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
open class RSAImpl(): RSA() {

    companion object {
        private const val RSA = "RSA"
        private const val SIGNATURE_ALGORITHM = "SHA1WithRSA"
    }

    override suspend fun generateKeyPair(
        keySize: KeySize,
        dispatcher: CoroutineDispatcher?,
        pkcsType: PKCSType,
    ): KotlinResponse<RSAKeyPair, ResponseError> {
        try {
            val generator: KeyPairGenerator = KeyPairGenerator.getInstance(RSA)
            generator.initialize(keySize.value, SecureRandom())

            val keys: KeyPair = dispatcher?.let {
                withContext(it) {
                    generator.genKeyPair()
                }
            } ?: generator.genKeyPair()

            if (pkcsType is PKCSType.PKCS8) {
                return KotlinResponse.Success(
                    RSAKeyPair(
                        RsaPrivateKey(keys.private.encoded.encodeBase64ToByteArray().toCharArray()),
                        RsaPublicKey(keys.public.encoded.encodeBase64ToByteArray().toCharArray())
                    )
                ).also {
                    keys.private.encoded?.fill('0'.toByte())
                }
            }

            val rsaPem = RSA_PEM(keys.public as RSAPublicKey, keys.private as RSAPrivateKey)

            return KotlinResponse.Success(
                RSAKeyPair(
                    RsaPrivateKey(rsaPem.ToPEM_PKCS1_Bytes(false).toCharArray()),
                    RsaPublicKey(rsaPem.ToPEM_PKCS1_Bytes(true).toCharArray()),
                )
            ).also {
                keys.private.encoded?.fill('0'.toByte())
                rsaPem.clear()
            }

        } catch (e: Exception) {
            return KotlinResponse.Error(ResponseError("RSA Key generation failure", e))
        }
    }

    @OptIn(RawPasswordAccess::class)
    override suspend fun decrypt(
        rsaPrivateKey: RsaPrivateKey,
        text: EncryptedString,
        dispatcher: CoroutineDispatcher,
    ): KotlinResponse<UnencryptedByteArray, ResponseError> {
        if (text.value.isEmpty()) {
            return KotlinResponse.Error(
                ResponseError("EncryptedString was empty")
            )
        }

        val dataBytes: ByteArray = text.value.decodeBase64ToArray()
            ?: return KotlinResponse.Error(
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
                        val toIndex: Int = if ( (fromIndex + blockSize) <= dataBytes.size ) {
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
                            val cipher: Cipher = Cipher.getInstance(RSA)
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
                            ba.fill('0'.toByte())
                        }
                        rsaPem.clear()

                    }

                    buffer.array().let { decrypted ->
                        decrypted.copyOfRange(fromIndex = 0, toIndex = finalSize).also {
                            decrypted.fill('0'.toByte())
                        }
                    }

                } else {

                    try {
                        val cipher: Cipher = Cipher.getInstance(RSA)
                        cipher.init(Cipher.DECRYPT_MODE, rsaPem.rsaPrivateKey)
                        cipher.doFinal(dataBytes)
                    } finally {
                        rsaPem.clear()
                    }

                }

            }

            KotlinResponse.Success(UnencryptedByteArray(decrypted))
        } catch (e: Exception) {
            KotlinResponse.Error(ResponseError("Decryption failed", e))
        }
    }

    @OptIn(UnencryptedDataAccess::class)
    override suspend fun encrypt(
        rsaPublicKey: RsaPublicKey,
        text: UnencryptedString,
        formatOutput: Boolean,
        dispatcher: CoroutineDispatcher
    ): KotlinResponse<EncryptedString, ResponseError> {
        if (text.value.isEmpty()) {
            return KotlinResponse.Error(
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
                            val cipher: Cipher = Cipher.getInstance(RSA)
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
                        val cipher: Cipher = Cipher.getInstance(RSA)
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

            KotlinResponse.Success(EncryptedString(string))
        } catch (e: Exception) {
            KotlinResponse.Error(ResponseError("Encryption failed", e))
        }
    }

    @OptIn(RawPasswordAccess::class)
    override suspend fun sign(
        rsaPrivateKey: RsaPrivateKey,
        text: String,
        dispatcher: CoroutineDispatcher
    ): KotlinResponse<RsaSignedString, ResponseError> {
        if (text.isEmpty()) {
            return KotlinResponse.Error(
                ResponseError("String value to sign was empty")
            )
        }

        return try {
            val signed: ByteArray = withContext(dispatcher) {

                val rsaPem: RSA_PEM = RSA_PEM.FromPEM(rsaPrivateKey.value, true)

                try {
                    val signature: Signature = Signature.getInstance(SIGNATURE_ALGORITHM)
                    signature.initSign(rsaPem.rsaPrivateKey)
                    signature.update(text.encodeToByteArray())
                    signature.sign()
                } finally {
                    rsaPem.clear()
                }
            }

            KotlinResponse.Success(
                RsaSignedString(
                    text,
                    RsaSignature(signed),
                )
            )
        } catch (e: Exception) {
            KotlinResponse.Error(ResponseError("Signing failed", e))
        }
    }

    override suspend fun verifySignature(
        rsaPublicKey: RsaPublicKey,
        signedString: RsaSignedString,
        dispatcher: CoroutineDispatcher
    ): KotlinResponse<Boolean, ResponseError> {
        if (signedString.signature.value.isEmpty()) {
            return KotlinResponse.Error(
                ResponseError("RsaSignature was empty")
            )
        }

        if (signedString.text.isEmpty()) {
            return KotlinResponse.Error(
                ResponseError("String value to verify was empty")
            )
        }

        return try {
            val verification: Boolean = withContext(dispatcher) {

                val rsaPem: RSA_PEM = RSA_PEM.FromPEM(rsaPublicKey.value, false)

                try {
                    val signVerify: Signature = Signature.getInstance(SIGNATURE_ALGORITHM)
                    signVerify.initVerify(rsaPem.rsaPublicKey)
                    signVerify.update(signedString.text.encodeToByteArray())
                    signVerify.verify(signedString.signature.value)
                } finally {
                    rsaPem.clear()
                }
            }

            KotlinResponse.Success(verification)
        } catch (e: Exception) {
            KotlinResponse.Error(ResponseError("Signature Verification failed", e))
        }
    }
}
