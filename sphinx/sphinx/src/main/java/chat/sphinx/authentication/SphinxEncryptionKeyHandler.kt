package chat.sphinx.authentication

import io.matthewnelson.concept_encryption_key.EncryptionKey
import io.matthewnelson.concept_encryption_key.EncryptionKeyHandler
import io.matthewnelson.k_openssl_common.clazzes.HashIterations
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SphinxEncryptionKeyHandler @Inject constructor(): EncryptionKeyHandler() {
    override suspend fun generateEncryptionKey(): EncryptionKey {
        // TODO: implement
        return copyAndStoreKey("testing".toCharArray())
    }

    override fun validateEncryptionKey(key: CharArray): EncryptionKey {
        // TODO: Validate key
        return copyAndStoreKey(key)
    }

    override fun getTestStringEncryptHashIterations(key: EncryptionKey): HashIterations {
        return HashIterations(20_000)
    }
}