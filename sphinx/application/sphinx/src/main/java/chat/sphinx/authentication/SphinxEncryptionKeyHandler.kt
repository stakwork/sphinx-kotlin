package chat.sphinx.authentication

import io.matthewnelson.concept_encryption_key.EncryptionKey
import io.matthewnelson.concept_encryption_key.EncryptionKeyHandler
import io.matthewnelson.k_openssl_common.annotations.RawPasswordAccess
import io.matthewnelson.k_openssl_common.clazzes.HashIterations
import io.matthewnelson.k_openssl_common.clazzes.Password
import io.matthewnelson.k_openssl_common.clazzes.clear
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SphinxEncryptionKeyHandler @Inject constructor(): EncryptionKeyHandler() {

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
        return getKeysToRestore()?.let { keys ->
            copyAndStoreKey(keys.privateKey.value, keys.publicKey.value)
        } ?: copyAndStoreKey("test private key".toCharArray(), "test public key".toCharArray())
    }

    override fun validateEncryptionKey(privateKey: CharArray, publicKey: CharArray): EncryptionKey {
        // TODO: Validate key
        return copyAndStoreKey(privateKey, publicKey)
    }

    override fun getTestStringEncryptHashIterations(privateKey: Password): HashIterations {
        return HashIterations(20_000)
    }
}