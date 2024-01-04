package chat.sphinx.feature_meme_server

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_query_meme_server.NetworkQueryMemeServer
import chat.sphinx.concept_repository_connect_manager.ConnectManagerRepository
import chat.sphinx.concept_repository_connect_manager.model.ConnectionManagerState
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.exception
import chat.sphinx.kotlin_response.message
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import chat.sphinx.logger.e
import chat.sphinx.wrapper_meme_server.*
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message_media.token.MediaHost
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private suspend inline fun AuthenticationStorage.removeToken(mediaHost: MediaHost) {
    removeString(
        String.format(MemeServerTokenHandlerImpl.MEME_SERVER_TOKEN, mediaHost.value)
    )
}

/**
 * Stores the [AuthenticationToken] along with its expiration time appended to it
 * */
private suspend inline fun AuthenticationStorage.persistToken(
    mediaHost: MediaHost,
    token: AuthenticationToken,
) {
    val nowPlus7Days = System.currentTimeMillis() + MemeServerTokenHandlerImpl._7_DAYS
    val tokenString = token.value + MemeServerTokenHandlerImpl.DELIMITER + nowPlus7Days.toString()
    putString(
        String.format(MemeServerTokenHandlerImpl.MEME_SERVER_TOKEN, mediaHost.value),
        tokenString
    )
}

class MemeServerTokenHandlerImpl(
    private val accountOwner: StateFlow<Contact?>,
    applicationScope: CoroutineScope,
    private val authenticationStorage: AuthenticationStorage,
    dispatchers: CoroutineDispatchers,
    private val networkQueryMemeServer: NetworkQueryMemeServer,
    private val LOG: SphinxLogger,
) : MemeServerTokenHandler(),
    CoroutineDispatchers by dispatchers {
    private var connectManagerRepository: ConnectManagerRepository? = null

    companion object {
        const val TAG = "MemeServerTokenHandlerImpl"

        // Persistent Storage Key
        const val MEME_SERVER_TOKEN = "MEME_SERVER_TOKEN_%s"

        const val DELIMITER = "|--|"

        @Suppress("ObjectPropertyName")
        const val _7_DAYS = 7 * 24 * 60 * 60 * 1000
    }

    private open inner class SynchronizedMap<K, V> {
        private val hashMap: MutableMap<K, V> = LinkedHashMap(1)
        private val lock = Mutex()
        open suspend fun <T> withLock(action: suspend (MutableMap<K, V>) -> T): T =
            lock.withLock {
                action(hashMap)
            }
    }

    /**
     * Generates & returns a [Mutex] for the given [MediaHost] in a synchronous manner
     * */
    private inner class SynchronizedLockMap : SynchronizedMap<MediaHost, Mutex>() {
        suspend fun getOrCreateLock(mediaHost: MediaHost): Mutex =
            super.withLock {
                it[mediaHost] ?: Mutex().also { mutex ->
                    it[mediaHost] = mutex
                }
            }

        override suspend fun <T> withLock(action: suspend (MutableMap<MediaHost, Mutex>) -> T): T {
            throw IllegalStateException("Use method getOrCreateLock instead")
        }
    }

    private val tokenCache = SynchronizedMap<MediaHost, AuthenticationToken?>()
    private val tokenLock = SynchronizedLockMap()

    override suspend fun retrieveAuthenticationToken(mediaHost: MediaHost): AuthenticationToken? {
        return tokenCache.withLock { it[mediaHost] } ?: tokenLock.getOrCreateLock(mediaHost)
            .withLock {
                tokenCache.withLock { it[mediaHost] } ?: retrieveAuthenticationTokenImpl(mediaHost)
                    .also { token ->
                        tokenCache.withLock { it[mediaHost] = token }
                    }
            }
    }

    override fun addListener(listener: ConnectManagerRepository) {
        connectManagerRepository = listener
    }

    private suspend fun retrieveAuthenticationTokenImpl(mediaHost: MediaHost): AuthenticationToken? {
        authenticationStorage.getString(
            String.format(MEME_SERVER_TOKEN, mediaHost.value),
            null
        ).let { tokenString ->
            return if (tokenString == null) {
                authenticateToHost(mediaHost)?.let { nnToken ->
                    authenticationStorage.persistToken(mediaHost, nnToken)

                    nnToken
                }
            } else {
                val data: Pair<AuthenticationToken, Long>? =
                    tokenString.split(DELIMITER).let { splits ->
                        splits.elementAtOrNull(0)?.toAuthenticationToken()?.let { token ->
                            splits.elementAtOrNull(1)?.toLongOrNull()?.let { expiration ->
                                Pair(token, expiration)
                            }
                        }
                    }

                data?.let { nnData ->
                    val now = System.currentTimeMillis()

                    if (now > nnData.second) {
                        authenticateToHost(mediaHost).let { token ->
                            if (token != null) {
                                authenticationStorage.persistToken(mediaHost, token)

                                token
                            } else {
                                authenticationStorage.removeToken(mediaHost)

                                null
                            }
                        }
                    } else {
                        LOG.d(
                            TAG,
                            """
                                MemeServerAuthenticationToken retrieved from persistent storage!
                                host: $mediaHost
                                token: ${nnData.first}
                                expiration (ms): ${nnData.second - now}
                            """.trimIndent()
                        )

                        nnData.first
                    }
                } ?: authenticateToHost(mediaHost).let { token ->
                    if (token != null) {
                        authenticationStorage.persistToken(mediaHost, token)

                        token
                    } else {
                        authenticationStorage.removeToken(mediaHost)

                        null
                    }
                }
            }
        }
    }

    private suspend fun authenticateToHost(mediaHost: MediaHost): AuthenticationToken? {
        var owner = accountOwner.value

        if (owner?.nodePubKey == null) {
            try {
                accountOwner.collect { contact ->
                    // suspend until account owner is available (either
                    // because we're awaiting a contact network refresh
                    // for the first time, or the DB has yet to be decrypted
                    if (contact != null) {
                        owner = contact
                        throw Exception()
                    }
                }
            } catch (e: Exception) {
            }

            delay(25L)
        }

        var token: AuthenticationToken? = null

        owner?.nodePubKey?.let { nodePubKey ->
            var id: AuthenticationId? = null
            var challenge: AuthenticationChallenge? = null

            networkQueryMemeServer.askAuthentication(mediaHost).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {}
                    is Response.Error -> {
                        LOG.e(TAG, loadResponse.message, loadResponse.exception)
                    }

                    is Response.Success -> {
                        id = loadResponse.value.id.toAuthenticationId()
                        connectManagerRepository?.singChallenge(loadResponse.value.challenge)
                        challenge = loadResponse.value.challenge.toAuthenticationChallenge()
                    }
                }
            }

            id?.let { nnId ->
                challenge?.let { nnChallenge ->

//                    networkQueryMemeServer.signChallenge(nnChallenge).collect { loadResponse ->
//                        @Exhaustive
//                        when (loadResponse) {
//                            is LoadResponse.Loading -> {}
//                            is Response.Error -> {
//                                LOG.e(TAG, loadResponse.message, loadResponse.exception)
//                            }
//                            is Response.Success -> {
//                                sig = loadResponse.value.sig.toAuthenticationSig()
//                            }
//                        }
//                    }

                    val sig: AuthenticationSig? = connectManagerRepository?.connectionManagerState
                        ?.firstOrNull { it is ConnectionManagerState.SignedChallenge }
                        ?.let { (it as ConnectionManagerState.SignedChallenge).authToken.toAuthenticationSig() }


                    sig?.let { nnSig ->

                        networkQueryMemeServer.verifyAuthentication(
                            nnId,
                            nnSig,
                            nodePubKey,
                            mediaHost,
                        ).collect { loadResponse ->
                            @Exhaustive
                            when (loadResponse) {
                                is LoadResponse.Loading -> {}
                                is Response.Error -> {
                                    LOG.e(TAG, loadResponse.message, loadResponse.exception)
                                }

                                is Response.Success -> {
                                    loadResponse.value.token.toAuthenticationToken()
                                        ?.let { nnToken ->
                                            token = nnToken
                                            LOG.d(
                                                TAG,
                                                """
                                                MemeServerAuthenticationToken acquired from server!
                                                host: $mediaHost
                                                token: $nnToken
                                            """.trimIndent()
                                            )
                                        }
                                }
                            }
                        }

                    }
                }
            }
        }
            return token
    }


    init {
        // Primes the default meme server token. If it's not persisted or
        // invalid (timed out), will suspend until the Account Owner is,
        // is available from the DB (meaning user has successfully authenticated).
        applicationScope.launch(mainImmediate) {
            retrieveAuthenticationToken(MediaHost.DEFAULT)
        }
    }
}
