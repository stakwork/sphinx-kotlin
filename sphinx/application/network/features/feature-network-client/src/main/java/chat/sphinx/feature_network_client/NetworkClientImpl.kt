package chat.sphinx.feature_network_client

import chat.sphinx.concept_network_client.NetworkClientClearedListener
import chat.sphinx.concept_network_client_cache.NetworkClientCache
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import io.matthewnelson.build_config.BuildConfigDebug
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class NetworkClientImpl(
    private val debug: BuildConfigDebug,
    private val cache: Cache,
    private val LOG: SphinxLogger,
): NetworkClientCache() {

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
    @Volatile
    private var client: OkHttpClient? = null
    private val clientLock = Mutex()

    override suspend fun getClient(): OkHttpClient =
        clientLock.withLock {
            client ?: createClientImpl().build()
                .also { client = it }
        }

    @Volatile
    private var cachingClient: OkHttpClient? = null
    private val cachingClientLock = Mutex()

    override suspend fun getCachingClient(): OkHttpClient =
        cachingClientLock.withLock {
            cachingClient ?: createClientImpl()
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

    private suspend fun createClientImpl(): OkHttpClient.Builder =
        OkHttpClient.Builder().let { builder ->

            builder.callTimeout(TIME_OUT * 3, TimeUnit.SECONDS)
            builder.connectTimeout(TIME_OUT, TimeUnit.SECONDS)
            builder.readTimeout(TIME_OUT, TimeUnit.SECONDS)
            builder.writeTimeout(TIME_OUT, TimeUnit.SECONDS)

            if (debug.value) {
                HttpLoggingInterceptor().let { interceptor ->
                    interceptor.level = HttpLoggingInterceptor.Level.BODY
                    builder.addNetworkInterceptor(interceptor)
                }
            }

            return builder
        }
}