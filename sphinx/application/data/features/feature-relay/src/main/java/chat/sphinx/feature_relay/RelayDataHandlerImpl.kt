package chat.sphinx.feature_relay

import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_relay.*
import chat.sphinx.wrapper_rsa.RsaPublicKey
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_encryption_key.EncryptionKeyHandler
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.annotations.UnencryptedDataAccess
import io.matthewnelson.crypto_common.clazzes.EncryptedString
import io.matthewnelson.crypto_common.clazzes.Password
import io.matthewnelson.crypto_common.clazzes.UnencryptedString
import io.matthewnelson.crypto_common.exceptions.DecryptionException
import io.matthewnelson.crypto_common.exceptions.EncryptionException
import io.matthewnelson.crypto_common.extensions.toHex
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import io.matthewnelson.k_openssl.KOpenSSL
import io.matthewnelson.k_openssl.algos.AES256CBC_PBKDF2_HMAC_SHA256
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class RelayDataHandlerImpl(
    private val authenticationStorage: AuthenticationStorage,
    private val authenticationCoreManager: AuthenticationCoreManager,
    private val dispatchers: CoroutineDispatchers,
    private val encryptionKeyHandler: EncryptionKeyHandler,
    private val torManager: TorManager,
    private val rsa: RSA,
) : RelayDataHandler(), CoroutineDispatchers by dispatchers {

    companion object {
        @Volatile
        private var relayUrlCache: RelayUrl? = null

        @Volatile
        private var tokenCache: AuthorizationToken? = null

        @Volatile
        private var relayTransportKeyCache: RsaPublicKey? = null

        @Volatile
        private var relayHMacKeyCache: RelayHMacKey? = null

        const val RELAY_URL_KEY = "RELAY_URL_KEY"
        const val RELAY_AUTHORIZATION_KEY = "RELAY_JWT_KEY"
        const val RELAY_TRANSPORT_ENCRYPTION_KEY = "RELAY_TRANSPORT_KEY"
        const val RELAY_H_MAC_KEY = "RELAY_H_MAC_KEY"
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

    private fun signHMacSha256(
        key: RelayHMacKey,
        text: String
    ) : String {
        val sha256HMac = Mac.getInstance("HMacSHA256")

        val secretKey = SecretKeySpec(
            key.value.toByteArray(), "HMacSHA256"
        )
        sha256HMac.init(secretKey)

        return sha256HMac.doFinal(
            text.toByteArray()
        ).toHex()
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

    override suspend fun persistRelayTransportKey(key: RsaPublicKey?): Boolean {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            persistRelayTransportKeyImpl(key, privateKey)
        } ?: false
    }

    suspend fun persistRelayTransportKeyImpl(key: RsaPublicKey?, privateKey: Password): Boolean {
        lock.withLock {
            if (key == null) {
                authenticationStorage.putString(RELAY_TRANSPORT_ENCRYPTION_KEY, null)
                relayTransportKeyCache = null
                return true
            } else {
                val encryptedTransportKey = try {
                    encryptData(privateKey, UnencryptedString(key.value.joinToString("")))
                } catch (e: Exception) {
                    return false
                }
                authenticationStorage.putString(RELAY_TRANSPORT_ENCRYPTION_KEY, encryptedTransportKey.value)
                relayTransportKeyCache = key
                return true
            }
        }
    }

    @OptIn(UnencryptedDataAccess::class)
    override suspend fun retrieveRelayTransportKey(): RsaPublicKey? {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            lock.withLock {
                relayTransportKeyCache ?: authenticationStorage.getString(RELAY_TRANSPORT_ENCRYPTION_KEY, null)
                    ?.let { encryptedTransportKey ->
                        try {
                            decryptData(privateKey, EncryptedString(encryptedTransportKey))
                                .value
                                .let { decryptedTransportKeyString ->
                                    val relayTransportKey = RsaPublicKey(decryptedTransportKeyString.toCharArray())
                                    relayTransportKeyCache = relayTransportKey
                                    relayTransportKey
                                }
                        } catch (e: Exception) {
                            null
                        }
                    }
            }
        }
    }

    override suspend fun persistRelayHMacKey(key: RelayHMacKey?): Boolean {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            persistRelayHMacKeyImpl(key, privateKey)
        } ?: false
    }

    private suspend fun persistRelayHMacKeyImpl(key: RelayHMacKey?, privateKey: Password): Boolean {
        lock.withLock {
            if (key == null) {
                authenticationStorage.putString(RELAY_H_MAC_KEY, null)
                relayHMacKeyCache = null
                return true
            } else {
                val encryptedHMacKey = try {
                    encryptData(privateKey, UnencryptedString(key.value))
                } catch (e: Exception) {
                    return false
                }
                authenticationStorage.putString(RELAY_H_MAC_KEY, encryptedHMacKey.value)
                relayHMacKeyCache = key
                return true
            }
        }
    }

    @OptIn(UnencryptedDataAccess::class)
    override suspend fun retrieveRelayHMacKey(): RelayHMacKey? {
        return authenticationCoreManager.getEncryptionKey()?.privateKey?.let { privateKey ->
            lock.withLock {
                relayHMacKeyCache ?: authenticationStorage.getString(RELAY_H_MAC_KEY, null)
                    ?.let { encryptedHMacKey ->
                        try {
                            decryptData(privateKey, EncryptedString(encryptedHMacKey))
                                .value
                                .let { decryptedHMacKeyString ->
                                    val relayHMacKey = RelayHMacKey(decryptedHMacKeyString)
                                    relayHMacKeyCache = relayHMacKey
                                    relayHMacKey
                                }
                        } catch (e: Exception) {
                            null
                        }
                    }
            }
        }
    }

    @OptIn(UnencryptedDataAccess::class)
    override suspend fun retrieveRelayTransportToken(
        authorizationToken: AuthorizationToken,
        transportKey: RsaPublicKey?
    ): TransportToken? {
        (transportKey ?: retrieveRelayTransportKey())?.let { key ->
            val unixTime = System.currentTimeMillis()
            val tokenAndTime = "${authorizationToken.value}|${unixTime}"

            val response = rsa.encrypt(
                key,
                UnencryptedString(tokenAndTime),
                formatOutput = false,
                dispatcher = default,
            )

            return when (response) {
                is Response.Error -> {
                    null
                }
                is Response.Success -> {
                    response.value.value
                        .toTransportToken()
                }
            }
        }
        return null
    }

    override suspend fun retrieveRelayRequestSignature(
        hMacKey: RelayHMacKey,
        method: String?,
        path: String?,
        bodyJsonString: String?
    ): RequestSignature? {
        if (
            method == null ||
            path == null
        ) {
            return null
        }

        val signedString = signHMacSha256(
            key = hMacKey,
            text = "${method!!}|${path!!}|${bodyJsonString ?: ""}"
        )

        return RequestSignature("sha256=$signedString")
    }
}
