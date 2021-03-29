package chat.sphinx.concept_crypto.rsa

import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.kotlin_response.ResponseError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Suppress("SpellCheckingInspection")
abstract class RSA {

    /**
     * Returns a base64 encoded private and public key pair.
     *
     * @param [keySize] The size keys to generate (2048, 4096, 8192)
     * @param [pkcsType] Either PKCS#1 or PKCS#8
     * @param [dispatcher] The dispatcher to generate keys on. If `null`, will
     *   run generation on whatever dispatcher [generateKeyPair] is called on.
     * */
    abstract suspend fun generateKeyPair(
        keySize: KeySize = KeySize._2048,
        dispatcher: CoroutineDispatcher? = Dispatchers.Default,
        pkcsType: PKCSType = PKCSType.PKCS1,
    ): KotlinResponse<RSAKeyPair, ResponseError>
}
