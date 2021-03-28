package chat.sphinx.concept_crypto.rsa

import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.kotlin_response.ResponseError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

abstract class RSA {

    /**
     * Returns a base64 encoded private and public key pair.
     * */
    abstract suspend fun generateKeyPair(
        rsaKeySize: RsaKeySize = RsaKeySize._2048,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): KotlinResponse<RSAKeyPair, ResponseError>
}
