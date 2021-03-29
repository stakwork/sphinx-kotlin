package chat.sphinx.feature_crypto.rsa

import chat.sphinx.concept_crypto.rsa.*
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.kotlin_response.ResponseError
import com.github.xiangyuecn.rsajava.RSA_PEM
import io.matthewnelson.k_openssl_common.extensions.toCharArray
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.base64.encodeBase64ToByteArray
import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

// TODO: Move to RSA_PEM once converted to Kotlin
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

class RSAImpl: RSA() {

    companion object {
        private const val RSA = "RSA"
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
                    rsaPem.ToPEM_PKCS1(false).toRsaPrivateKey(),
                    rsaPem.ToPEM_PKCS1(true).toRsaPublicKey(),
                )
            ).also {
                keys.private.encoded?.fill('0'.toByte())
                rsaPem.clear()
            }

        } catch (e: Exception) {
            return KotlinResponse.Error(ResponseError("RSA Key generation failure", e))
        }
    }
}
