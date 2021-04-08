package chat.sphinx.concept_crypto_rsa

import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_rsa.PKCSType
import chat.sphinx.wrapper_rsa.RSAKeyPair
import chat.sphinx.wrapper_rsa.RsaPrivateKey
import chat.sphinx.wrapper_rsa.RsaPublicKey
import chat.sphinx.wrapper_rsa.RsaSignedString
import io.matthewnelson.crypto_common.clazzes.EncryptedString
import io.matthewnelson.crypto_common.clazzes.UnencryptedByteArray
import io.matthewnelson.crypto_common.clazzes.UnencryptedString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Suppress("SpellCheckingInspection")
abstract class RSA {

    /**
     * Returns a base64 encoded private and public key pair.
     *
     * @param [keySize] The size keys to generate (1024, 2048, 3072, 4096,
     *   8192). Defaults to 2048
     * @param [pkcsType] Either PKCS#1 or PKCS#8. Defaults to PKCS#1
     * @param [dispatcher] The dispatcher to generate keys on. If `null`, will
     *   run generation on whatever dispatcher [generateKeyPair] is called on.
     * */
    abstract suspend fun generateKeyPair(
        keySize: KeySize = KeySize._2048,
        dispatcher: CoroutineDispatcher? = Dispatchers.Default,
        pkcsType: PKCSType = PKCSType.PKCS1,
    ): Response<RSAKeyPair, ResponseError>

    /**
     * Decrypts a base64 encoded [EncryptedString] value
     *
     * @param [rsaPrivateKey] The private key to decrypt with
     * @param [text] The base64 encoded string value to decrypt
     * @param [dispatcher] The dispatcher to use when decrypting
     * */
    abstract suspend fun decrypt(
        rsaPrivateKey: RsaPrivateKey,
        text: EncryptedString,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): Response<UnencryptedByteArray, ResponseError>

    /**
     * Encrypts an [UnencryptedString] value and returns a base64 encoded [EncryptedString]
     *
     * @param [rsaPublicKey] The public key to encrypt with
     * @param [text] The raw, unencrypted string value to encrypt
     * @param [formatOutput] `true`: format output to 64 chars per line, `false`: single line
     * @param [dispatcher] The dispathcer to use when encrypting
     * */
    abstract suspend fun encrypt(
        rsaPublicKey: RsaPublicKey,
        text: UnencryptedString,
        formatOutput: Boolean = false,
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): Response<EncryptedString, ResponseError>

    abstract suspend fun sign(
        rsaPrivateKey: RsaPrivateKey,
        text: String,
        algorithm: SignatureAlgorithm = SignatureAlgorithm.SHA256_with_RSA,
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): Response<RsaSignedString, ResponseError>

    abstract suspend fun verifySignature(
        rsaPublicKey: RsaPublicKey,
        signedString: RsaSignedString,
        algorithm: SignatureAlgorithm = SignatureAlgorithm.SHA256_with_RSA,
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): Response<Boolean, ResponseError>
}
