package chat.sphinx.feature_relay

import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.isOnionAddress
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
import okhttp3.HttpUrl.Companion.toHttpUrl

class RelayDataHandlerImpl(
    private val authenticationStorage: AuthenticationStorage,
    private val authenticationCoreManager: AuthenticationCoreManager,
    private val dispatchers: CoroutineDispatchers,
    private val encryptionKeyHandler: EncryptionKeyHandler,
    private val torManager: TorManager,
) : RelayDataHandler(), CoroutineDispatchers by dispatchers {

    companion object {
        @Volatile
        private var relayUrlCache: RelayUrl? = null

        @Volatile
        private var tokenCache: AuthorizationToken? = null

        const val RELAY_URL_KEY = "RELAY_URL_KEY"
        const val RELAY_AUTHORIZATION_KEY = "RELAY_JWT_KEY"
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

    override suspend fun persistRelayUrl(url: RelayUrl): Boolean {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            persistRelayUrlImpl(url, privateKey)
        } ?: false
    }

    suspend fun persistRelayUrlImpl(url: RelayUrl, privateKey: Password): Boolean {
        lock.withLock {
            val formattedUrl = formatRelayUrl(url)
            val encryptedRelayUrl = try {
                encryptData(privateKey, UnencryptedString(formattedUrl.value))
            } catch (e: Exception) {
                return false
            }

            authenticationStorage.putString(RELAY_URL_KEY, encryptedRelayUrl.value)
            torManager.setTorRequired(formattedUrl.isOnionAddress)
            relayUrlCache = formattedUrl
            return true
        }
    }

    override fun formatRelayUrl(relayUrl: RelayUrl): RelayUrl {
        return try {
            relayUrl.value.toHttpUrl()

            // is valid url with scheme
            relayUrl
        } catch (e: IllegalArgumentException) {

            // does not contain http, https... check if it's an onion address
            if (relayUrl.isOnionAddress) {
                RelayUrl("http://${relayUrl.value}")
            } else {
                RelayUrl("https://${relayUrl.value}")
            }
        }
    }

    @OptIn(UnencryptedDataAccess::class)
    override suspend fun retrieveRelayUrl(): RelayUrl? {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            lock.withLock {
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
    }

    override suspend fun persistAuthorizationToken(token: AuthorizationToken?): Boolean {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            persistJavaWebTokenImpl(token, privateKey)
        } ?: false
    }

    /**
     * If sending `null` argument for [token], an empty [Password] is safe to send as this
     * will only clear the token from storage and not encrypt anything.
     * */
    suspend fun persistJavaWebTokenImpl(token: AuthorizationToken?, privateKey: Password): Boolean {
        lock.withLock {
            if (token == null) {
                authenticationStorage.putString(RELAY_AUTHORIZATION_KEY, null)
                tokenCache = null
                return true
            } else {
                val encryptedJWT = try {
                    encryptData(privateKey, UnencryptedString(token.value))
                } catch (e: Exception) {
                    return false
                }

                authenticationStorage.putString(RELAY_AUTHORIZATION_KEY, encryptedJWT.value)
                tokenCache = token
                return true
            }
        }
    }

    @OptIn(UnencryptedDataAccess::class)
    override suspend fun retrieveAuthorizationToken(): AuthorizationToken? {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            lock.withLock {
                tokenCache ?: authenticationStorage.getString(RELAY_AUTHORIZATION_KEY, null)
                    ?.let { encryptedJwtString ->
                        try {
                            decryptData(privateKey, EncryptedString(encryptedJwtString))
                                .value
                                .let { decryptedJwtString ->
                                    val token = AuthorizationToken(decryptedJwtString)
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
}
