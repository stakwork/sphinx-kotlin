package chat.sphinx.authentication

import chat.sphinx.concept_crypto_rsa.KeySize
import chat.sphinx.wrapper_rsa.PKCSType
import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.exception
import chat.sphinx.kotlin_response.message
import io.matthewnelson.concept_encryption_key.EncryptionKey
import io.matthewnelson.concept_encryption_key.EncryptionKeyException
import io.matthewnelson.concept_encryption_key.EncryptionKeyHandler
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.clazzes.HashIterations
import io.matthewnelson.crypto_common.clazzes.Password
import io.matthewnelson.crypto_common.clazzes.clear

class SphinxEncryptionKeyHandler(private val rsa: RSA): EncryptionKeyHandler() {

    private var keysToRestore: RestoreKeyHolder? = null

    private class RestoreKeyHolder(val privateKey: Password, val publicKey: Password)

    fun setKeysToRestore(privateKey: Password, publicKey: Password) {
        synchronized(this) {
            keysToRestore = RestoreKeyHolder(privateKey, publicKey)
        }
    }

    fun clearKeysToRestore() {
        synchronized(this) {
            keysToRestore?.privateKey?.clear()
            keysToRestore?.publicKey?.clear()
            keysToRestore = null
        }
    }

    private fun getKeysToRestore(): RestoreKeyHolder? =
        synchronized(this) {
            keysToRestore
        }

    @OptIn(RawPasswordAccess::class)
    override suspend fun generateEncryptionKey(): EncryptionKey {
        getKeysToRestore()?.let { keys ->
            return copyAndStoreKey(keys.privateKey.value, keys.publicKey.value)
        }

         val response = rsa.generateKeyPair(
             keySize = KeySize._2048,
             dispatcher = null, // generateEncryptionKey is called on Dispatcher.Default
             pkcsType = PKCSType.PKCS1,
         )

        if (response is Response.Success) {
            return copyAndStoreKey(
                response.value.privateKey.value,
                response.value.publicKey.value,
            ).also {
                response.value.privateKey.value.fill('0')
            }
        } else {
            throw (response as Response.Error).exception
                ?: EncryptionKeyException(response.message)
        }
    }

    override fun validateEncryptionKey(privateKey: CharArray, publicKey: CharArray): EncryptionKey {
        // TODO: Validate key
        return copyAndStoreKey(privateKey, publicKey)
    }

    override fun getTestStringEncryptHashIterations(privateKey: Password): HashIterations {
        return HashIterations(20_000)
    }
}
