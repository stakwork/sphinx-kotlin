package chat.sphinx.authentication

import io.matthewnelson.concept_encryption_key.EncryptionKey
import io.matthewnelson.concept_encryption_key.EncryptionKeyHandler
import io.matthewnelson.k_openssl_common.clazzes.HashIterations
import io.matthewnelson.k_openssl_common.clazzes.Password
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SphinxEncryptionKeyHandler @Inject constructor(): EncryptionKeyHandler() {
    override suspend fun generateEncryptionKey(): EncryptionKey {
        // TODO: implement
        return copyAndStoreKey("test private key".toCharArray(), "test public key".toCharArray())
    }

    override fun validateEncryptionKey(privateKey: CharArray, publicKey: CharArray): EncryptionKey {
        // TODO: Validate key
        return copyAndStoreKey(privateKey, publicKey)
    }

    override fun getTestStringEncryptHashIterations(privateKey: Password): HashIterations {
        return HashIterations(20_000)
    }
}