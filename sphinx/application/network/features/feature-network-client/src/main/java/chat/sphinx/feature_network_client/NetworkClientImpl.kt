package chat.sphinx.feature_network_client

import chat.sphinx.concept_network_client.NetworkClient
import io.matthewnelson.build_config.BuildConfigDebug
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class NetworkClientImpl(private val debug: BuildConfigDebug): NetworkClient() {

    companion object {
        const val TIME_OUT = 30L
    }

    @Volatile
    private var client: OkHttpClient? = null
    private val lock = Mutex()

    override suspend fun getClient(): OkHttpClient =
        lock.withLock {
            client ?: createClientImpl()
                .also { client = it }
        }

    // TODO: For future Tor implementation where variability in the
    //  SOCKS Proxy can change depending on network state and if the
    //  SOCKS Port is set to auto.
    suspend fun createClient(): OkHttpClient =
        lock.withLock {
            createClientImpl()
        }

    private suspend fun createClientImpl(): OkHttpClient =
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

            return builder.build()
                .also { client = it }
        }
}