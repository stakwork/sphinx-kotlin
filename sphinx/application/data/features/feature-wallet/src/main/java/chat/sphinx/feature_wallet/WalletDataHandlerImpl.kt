package chat.sphinx.feature_wallet

import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.wrapper_lightning.WalletMnemonic
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_encryption_key.EncryptionKeyHandler
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import io.matthewnelson.k_openssl.KOpenSSL
import io.matthewnelson.k_openssl.algos.AES256CBC_PBKDF2_HMAC_SHA256
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.annotations.UnencryptedDataAccess
import io.matthewnelson.crypto_common.clazzes.EncryptedString
import io.matthewnelson.crypto_common.clazzes.Password
import io.matthewnelson.crypto_common.clazzes.UnencryptedString
import io.matthewnelson.crypto_common.exceptions.DecryptionException
import io.matthewnelson.crypto_common.exceptions.EncryptionException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WalletDataHandlerImpl(
    private val authenticationStorage: AuthenticationStorage,
    private val authenticationCoreManager: AuthenticationCoreManager,
    private val dispatchers: CoroutineDispatchers,
    private val encryptionKeyHandler: EncryptionKeyHandler,
) : WalletDataHandler(), CoroutineDispatchers by dispatchers {

    companion object {
        @Volatile
        private var walletMnemonicCache: WalletMnemonic? = null

        const val WALLET_MNEMONIC_KEY = "WALLET_MNEMONIC_KEY"
    }

    private val kOpenSSL: KOpenSSL by lazy {
        AES256CBC_PBKDF2_HMAC_SHA256()
    }

    @OptIn(UnencryptedDataAccess::class, RawPasswordAccess::class)
    @Throws(EncryptionException::class, IllegalArgumentException::class)
    private suspend fun encryptData(
        privateKey: Password,
        data: UnencryptedString
    ): EncryptedString {
        if (privateKey.value.isEmpty()) {
            throw IllegalArgumentException("Private Key cannot be empty")
        }
        if (data.value.isEmpty()) {
            throw IllegalArgumentException("Data cannot be empty")
        }

        return try {
            kOpenSSL.encrypt(
                privateKey,
                encryptionKeyHandler.getTestStringEncryptHashIterations(privateKey),
                data,
                default
            )
        } catch (e: Exception) {
            throw EncryptionException("Failed to encrypt data", e)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @OptIn(UnencryptedDataAccess::class, RawPasswordAccess::class)
    @Throws(DecryptionException::class, IllegalArgumentException::class)
    private suspend fun decryptData(
        privateKey: Password,
        data: EncryptedString
    ): UnencryptedString {
        if (privateKey.value.isEmpty()) {
            throw IllegalArgumentException("Private Key cannot be empty")
        }
        if (data.value.isEmpty()) {
            throw IllegalArgumentException("Data cannot be empty")
        }

        return try {
            kOpenSSL.decrypt(
                privateKey,
                encryptionKeyHandler.getTestStringEncryptHashIterations(privateKey),
                data,
                default
            )
        } catch (e: Exception) {
            throw DecryptionException("Failed to decrypt data", e)
        }
    }

    private val lock = Mutex()

    override suspend fun persistWalletMnemonic(mnemonic: WalletMnemonic): Boolean {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            persistWalletMnemonicImpl(mnemonic, privateKey)
        } ?: false
    }

    private suspend fun persistWalletMnemonicImpl(mnemonic: WalletMnemonic, privateKey: Password): Boolean {
        lock.withLock {
            val encryptedMnemonic = try {
                encryptData(privateKey, UnencryptedString(mnemonic.value))
            } catch (e: Exception) {
                return false
            }

            authenticationStorage.putString(WALLET_MNEMONIC_KEY, encryptedMnemonic.value)
            walletMnemonicCache = mnemonic
            return true
        }
    }

    @OptIn(UnencryptedDataAccess::class)
    override suspend fun retrieveWalletMnemonic(): WalletMnemonic? {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            lock.withLock {
                walletMnemonicCache ?: authenticationStorage.getString(WALLET_MNEMONIC_KEY, null)
                    ?.let { encryptedWalletMnemonic ->
                        try {
                            decryptData(privateKey, EncryptedString(encryptedWalletMnemonic))
                                .value
                                .let { decryptedWalletMnemonic ->
                                    val mnemonic = WalletMnemonic(decryptedWalletMnemonic)
                                    walletMnemonicCache = mnemonic
                                    mnemonic
                                }
                        } catch (e: Exception) {
                            null
                        }
                    }
            }
        }
    }
}
