package chat.sphinx.feature_crypto.rsa

import chat.sphinx.concept_crypto.rsa.RSA
import chat.sphinx.concept_crypto.rsa.RsaKeySize
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.kotlin_response.ResponseError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.security.KeyPair
import java.security.KeyPairGenerator

class RSAImpl: RSA() {

    companion object {
        private const val RSA = "RSA"
    }

    override suspend fun generateKeyPair(
        rsaKeySize: RsaKeySize,
        dispatcher: CoroutineDispatcher
    ): KotlinResponse<KeyPair, ResponseError> {
        return try {
            val generator = KeyPairGenerator.getInstance(RSA)
            generator.initialize(rsaKeySize.value)

            val keys = withContext(dispatcher) {
                generator.genKeyPair()
            }

            KotlinResponse.Success(keys)
        } catch (e: Exception) {
            KotlinResponse.Error(ResponseError("RSA Key generation failure", e))
        }
    }
}