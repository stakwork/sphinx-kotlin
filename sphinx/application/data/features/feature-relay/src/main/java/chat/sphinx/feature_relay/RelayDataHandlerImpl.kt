package chat.sphinx.feature_relay

import chat.sphinx.concept_relay.JavaWebToken
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_relay.RelayUrl
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_encryption_key.EncryptionKeyHandler
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import io.matthewnelson.k_openssl.KOpenSSL
import io.matthewnelson.k_openssl.algos.AES256CBC_PBKDF2_HMAC_SHA256
import io.matthewnelson.k_openssl_common.annotations.RawPasswordAccess
import io.matthewnelson.k_openssl_common.annotations.UnencryptedDataAccess
import io.matthewnelson.k_openssl_common.clazzes.EncryptedString
import io.matthewnelson.k_openssl_common.clazzes.Password
import io.matthewnelson.k_openssl_common.clazzes.UnencryptedString
import io.matthewnelson.k_openssl_common.exceptions.DecryptionException
import io.matthewnelson.k_openssl_common.exceptions.EncryptionException

class RelayDataHandlerImpl(
    private val authenticationStorage: AuthenticationStorage,
    private val authenticationCoreManager: AuthenticationCoreManager,
    private val dispatchers: CoroutineDispatchers,
    private val encryptionKeyHandler: EncryptionKeyHandler
): RelayDataHandler() {

    companion object {
        @Volatile
        private var relayUrlCache: RelayUrl? = null

        @Volatile
        private var tokenCache: JavaWebToken? = null

        const val RELAY_URL_KEY = "RELAY_URL_KEY"
        const val RELAY_JWT_KEY = "RELAY_JWT_KEY"
    }

    private val kOpenSSL: KOpenSSL by lazy {
        AES256CBC_PBKDF2_HMAC_SHA256()
    }

    @OptIn(UnencryptedDataAccess::class, RawPasswordAccess::class)
    @Throws(EncryptionException::class, IllegalArgumentException::class)
    private suspend fun encryptData(privateKey: Password, data: UnencryptedString): EncryptedString {
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
                dispatchers.default
            )
        } catch (e: Exception) {
            throw EncryptionException("Failed to encrypt data", e)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @OptIn(UnencryptedDataAccess::class, RawPasswordAccess::class)
    @Throws(DecryptionException::class, IllegalArgumentException::class)
    private suspend fun decryptData(privateKey: Password, data: EncryptedString): UnencryptedString {
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
                dispatchers.default
            )
        } catch (e: Exception) {
            throw DecryptionException("Failed to decrypt data", e)
        }
    }

    @Synchronized
    override suspend fun persistRelayUrl(url: RelayUrl): Boolean {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            persistRelayUrlImpl(url, privateKey)
        } ?: false
    }

    @Synchronized
    suspend fun persistRelayUrlImpl(url: RelayUrl, privateKey: Password): Boolean {
        val encryptedRelayUrl = try {
            encryptData(privateKey, UnencryptedString(url.value))
        } catch (e: Exception) {
            return false
        }

        authenticationStorage.putString(RELAY_URL_KEY, encryptedRelayUrl.value)
        relayUrlCache = url
        return true
    }

    @Synchronized
    @OptIn(UnencryptedDataAccess::class)
    override suspend fun retrieveRelayUrl(): RelayUrl? {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            relayUrlCache ?: authenticationStorage.getString(RELAY_URL_KEY, null)
                ?.let { encryptedUrlString ->
                try {
                    decryptData(privateKey, EncryptedString(encryptedUrlString))
                        .value
                        .let { decryptedUrlString ->
                            val url = RelayUrl(decryptedUrlString)
                            relayUrlCache = url
                            url
                        }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    @Synchronized
    override suspend fun persistJavaWebToken(token: JavaWebToken?): Boolean {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            persistJavaWebTokenImpl(token, privateKey)
        } ?: false
    }

    /**
     * If sending `null` argument for [token], an empty [Password] is safe to send as this
     * will only clear the token from storage and not encrypt anything.
     * */
    @Synchronized
    suspend fun persistJavaWebTokenImpl(token: JavaWebToken?, privateKey: Password): Boolean {
        if (token == null) {
            authenticationStorage.putString(RELAY_JWT_KEY, null)
            tokenCache = null
            return true
        } else {
            val encryptedJWT = try {
                encryptData(privateKey, UnencryptedString(token.value))
            } catch (e: Exception) {
                return false
            }

            authenticationStorage.putString(RELAY_JWT_KEY, encryptedJWT.value)
            tokenCache = token
            return true
        }
    }

    @Synchronized
    @OptIn(UnencryptedDataAccess::class)
    override suspend fun retrieveJavaWebToken(): JavaWebToken? {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            tokenCache ?: authenticationStorage.getString(RELAY_JWT_KEY, null)
                ?.let { encryptedJwtString ->
                    try {
                        decryptData(privateKey, EncryptedString(encryptedJwtString))
                            .value
                            .let { decryptedJwtString ->
                                val token = JavaWebToken(decryptedJwtString)
                                tokenCache = token
                                token
                            }
                    } catch (e: Exception) {
                        null
                    }
                }
        }
    }
}