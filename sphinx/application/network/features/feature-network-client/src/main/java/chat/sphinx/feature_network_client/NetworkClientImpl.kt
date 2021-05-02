package chat.sphinx.feature_network_client

import chat.sphinx.concept_network_client_cache.NetworkClientCache
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
): NetworkClientCache() {

    companion object {
        const val TIME_OUT = 15L
        const val PING_INTERVAL = 25L

        const val CACHE_CONTROL = "Cache-Control"
        const val MAX_STALE = "public, max-stale=$MAX_STALE_VALUE"
    }

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

    private var callback: () -> Unit? = {}
    override fun addOnClientClearedCallback(onClear: () -> Unit) {
        callback = onClear
    }

    override fun removeOnClientClearedCallback() {
        callback = {}
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