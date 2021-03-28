package chat.sphinx.feature_crypto.rsa

import chat.sphinx.concept_crypto.rsa.*
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.kotlin_response.ResponseError
import io.matthewnelson.k_openssl_common.extensions.toCharArray
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.base64.encodeBase64ToByteArray
import java.security.KeyPairGenerator

class RSAImpl: RSA() {

    companion object {
        private const val RSA = "RSA"
    }

    override suspend fun generateKeyPair(
        rsaKeySize: RsaKeySize,
        dispatcher: CoroutineDispatcher?
    ): KotlinResponse<RSAKeyPair, ResponseError> {
        return try {
            val generator = KeyPairGenerator.getInstance(RSA)
            generator.initialize(rsaKeySize.value)

            val keys = dispatcher?.let {
                withContext(it) {
                    generator.genKeyPair()
                }
            } ?: generator.genKeyPair()

            KotlinResponse.Success(
                RSAKeyPair(
                    RsaPrivateKey(keys.private.encoded.encodeBase64ToByteArray().toCharArray()),
                    RsaPublicKey(keys.public.encoded.encodeBase64ToByteArray().toCharArray())
                )
            ).also {
                keys.private.encoded.fill('0'.toByte())
            }
        } catch (e: Exception) {
            KotlinResponse.Error(ResponseError("RSA Key generation failure", e))
        }
    }
}