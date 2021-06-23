package chat.sphinx.feature_network_client

import chat.sphinx.concept_network_client.NetworkClientClearedListener
import chat.sphinx.concept_network_client_cache.NetworkClientCache
import chat.sphinx.concept_network_tor.*
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class NetworkClientImpl(
    private val debug: BuildConfigDebug,
    private val cache: Cache,
    private val dispatchers: CoroutineDispatchers,
    redactedLoggingHeaders: RedactedLoggingHeaders?,
    private val torManager: TorManager,
    private val LOG: SphinxLogger,
) : NetworkClientCache(),
    TorManagerListener,
    CoroutineDispatchers by dispatchers
{

    companion object {
        const val TAG = "NetworkClientImpl"

        const val TIME_OUT = 15L
        const val PING_INTERVAL = 25L

        const val CACHE_CONTROL = "Cache-Control"
        const val MAX_STALE = "public, max-stale=$MAX_STALE_VALUE"
    }

    /////////////////
    /// Listeners ///
    /////////////////
    private inner class SynchronizedListenerHolder {
        private val listeners: LinkedHashSet<NetworkClientClearedListener> = LinkedHashSet(0)

        fun addListener(listener: NetworkClientClearedListener): Boolean {
            synchronized(this) {
                val bool = listeners.add(listener)
                if (bool) {
                    LOG.d(TAG, "Listener ${listener.javaClass.simpleName} registered")
                }
                return bool
            }
        }

        fun removeListener(listener: NetworkClientClearedListener): Boolean {
            synchronized(this) {
                val bool = listeners.remove(listener)
                if (bool) {
                    LOG.d(TAG, "Listener ${listener.javaClass.simpleName} removed")
                }
                return bool
            }
        }

        fun clear() {
            synchronized(this) {
                if (listeners.isNotEmpty()) {
                    listeners.clear()
                    LOG.d(TAG, "Listeners cleared")
                }
            }
        }

        fun dispatchClearedEvent() {
            synchronized(this) {
                for (listener in listeners) {
                    listener.networkClientCleared()
                }
            }
        }

        val hasListeners: Boolean
            get() = synchronized(this) {
                listeners.isNotEmpty()
            }
    }

    private val synchronizedListeners = SynchronizedListenerHolder()

    override fun addListener(listener: NetworkClientClearedListener): Boolean {
        return synchronizedListeners.addListener(listener)
    }

    override fun removeListener(listener: NetworkClientClearedListener): Boolean {
        return synchronizedListeners.removeListener(listener)
    }

    ///////////////
    /// Clients ///
    ///////////////
    class RedactedLoggingHeaders(val headers: List<String>)

    @Volatile
    private var client: OkHttpClient? = null
    @Volatile
    private var clearedClient: OkHttpClient? = null

    private val clientLock = Mutex()
    private var currentClientSocksProxyAddress: SocksProxyAddress? = null

    private val cryptoInterceptor: CryptoInterceptor by lazy {
        CryptoInterceptor()
    }

    private val loggingInterceptor: HttpLoggingInterceptor? by lazy {
        if (debug.value) {
            HttpLoggingInterceptor().let { interceptor ->
                interceptor.level = HttpLoggingInterceptor.Level.BODY
                redactedLoggingHeaders?.headers?.let { list ->
                    for (header in list) {
                        if (header.isNotEmpty()) {
                            interceptor.redactHeader(header)
                        }
                    }
                }
                interceptor
            }
        } else {
            null
        }
    }

    override suspend fun getClient(): OkHttpClient =
        clientLock.withLock {
            client ?: (clearedClient?.newBuilder()?.also { clearedClient = null } ?: OkHttpClient.Builder()).apply {
                connectTimeout(TIME_OUT, TimeUnit.SECONDS)
                readTimeout(TIME_OUT, TimeUnit.SECONDS)
                writeTimeout(TIME_OUT, TimeUnit.SECONDS)

                if (torManager.isTorRequired() == true) {

                    torManager.startTor()

                    var socksPortJob: Job? = null
                    var torStateJob: Job? = null

                    coroutineScope {
                        socksPortJob = launch(mainImmediate) {
                            try {
                                // wait for Tor to start and publish its socks address after
                                // being bootstrapped.
                                torManager.socksProxyAddressStateFlow.collect { socksAddress ->
                                    if (socksAddress != null) {
                                        proxy(
                                            Proxy(
                                                Proxy.Type.SOCKS,
                                                InetSocketAddress(socksAddress.host, socksAddress.port)
                                            )
                                        )
                                        currentClientSocksProxyAddress = socksAddress

                                        throw Exception()
                                    }
                                }
                            } catch (e: Exception) {}
                        }

                        torStateJob = launch(mainImmediate) {
                            var retry: Int = 3
                            delay(250L)
                            try {
                                torManager.torStateFlow.collect { state ->
                                    if (state is TorState.Off) {
                                        if (retry >= 0) {
                                            LOG.d(TAG, "Tor failed to start, retrying: $retry")
                                            torManager.startTor()
                                            retry--
                                        } else {
                                            socksPortJob?.cancel()
                                            throw Exception()
                                        }
                                    }

                                    if (state is TorState.On) {
                                        throw Exception()
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                    }

                    torStateJob?.join()
                    socksPortJob?.join()

                    // Tor failed to start, but we still want to set the proxy port
                    // so we don't leak _any_ network requests.
                    if (
                        currentClientSocksProxyAddress == null &&
                        torManager.torStateFlow.value == TorState.Off
                    ) {
                        val socksPort: Int = try {
                            // could be `auto` if user set it to that, which will mean we won't
                            // know the port just yet so use the default setting,
                            torManager.getSocksPortSetting().toInt()
                        } catch (e: NumberFormatException) {
                            TorManager.DEFAULT_SOCKS_PORT
                        }

                        proxy(
                            Proxy(
                                Proxy.Type.SOCKS,
                                InetSocketAddress("127.0.0.1", socksPort)
                            )
                        )
                        currentClientSocksProxyAddress = SocksProxyAddress("127.0.0.1:$socksPort")
                    }

                    // check again in case the setting has changed
                    if (torManager.isTorRequired() != true) {
                        proxy(null)
                        currentClientSocksProxyAddress = null

                        torManager.stopTor()

                        LOG.d(
                            TAG,
                            """
                                Tor requirement changed to false while building the network client.
                                Proxy settings removed and stopTor called.
                            """.trimIndent()
                        )
                    } else {
                        LOG.d(TAG, "Client built with $currentClientSocksProxyAddress")
                    }
                } else {
                    currentClientSocksProxyAddress = null
                    proxy(null)
                }

                if (!interceptors().contains(cryptoInterceptor)) {
                    addInterceptor(cryptoInterceptor)
                }

                loggingInterceptor?.let { nnInterceptor ->
                    if (!networkInterceptors().contains(nnInterceptor)) {
                        addNetworkInterceptor(nnInterceptor)
                    }
                }

            }
                .build()
                .also { client = it }
        }

    @Volatile
    private var cachingClient: OkHttpClient? = null
    private val cachingClientLock = Mutex()

    override suspend fun getCachingClient(): OkHttpClient =
        cachingClientLock.withLock {
            cachingClient ?: getClient().newBuilder()
                .cache(cache)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header(CACHE_CONTROL, MAX_STALE)
                        .build()
                    chain.proceed(request)
                }
                .build()
                .also { cachingClient = it }
        }

    //////////////////////////
    /// TorManagerListener ///
    //////////////////////////
    override suspend fun onTorRequirementChange(required: Boolean) {
        cachingClientLock.withLock {
            clientLock.withLock {
                client?.let { nnClient ->
                    if (required) {
                        if (currentClientSocksProxyAddress == null) {
                            clearedClient = nnClient
                            client = null
                            cachingClient = null
                            synchronizedListeners.dispatchClearedEvent()
                        }
                    } else {
                        if (currentClientSocksProxyAddress != null) {
                            clearedClient = nnClient
                            client = null
                            cachingClient = null
                            synchronizedListeners.dispatchClearedEvent()
                        }
                    }
                }
            }
        }
    }

    override suspend fun onTorSocksProxyAddressChange(socksProxyAddress: SocksProxyAddress?) {
        if (socksProxyAddress == null) {
            return
        }

        cachingClientLock.withLock {
            clientLock.withLock {
                client?.let { nnClient ->
                    // We don't want to close down the client,
                    // just move it temporarily so it forces a rebuild
                    // with the new proxy settings
                    clearedClient = nnClient
                    client = null
                    cachingClient = null
                    synchronizedListeners.dispatchClearedEvent()
                }
            }
        }
    }

    init {
        torManager.addTorManagerListener(this)
    }
}
