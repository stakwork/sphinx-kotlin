package chat.sphinx.feature_image_loader_android

import android.content.Context
import android.os.Build
import android.widget.ImageView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_network_client_cache.NetworkClientCache
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.fetch.VideoFrameFileFetcher
import coil.fetch.VideoFrameUriFetcher
import coil.request.ImageRequest
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

class ImageLoaderAndroid(
    context: Context,
    private val dispatchers: CoroutineDispatchers,
    private val networkClientCache: NetworkClientCache
): ImageLoader<ImageView>() {

    private val appContext: Context = context.applicationContext

    @Volatile
    private var loader: coil.ImageLoader? = null
    private val loaderLock = Mutex()

    override suspend fun load(imageView: ImageView, url: String): Disposable {
        loaderLock.withLock {
            if (networkClientCache.isCachingClientCleared) {
                loader = null
            }

            // Future-proofing:
            // Always retrieve the client, as Tor may be enabled but
            // in a suspended state and we don't want to do any requests
            // w/o a proxied client.
            networkClientCache.getCachingClient().let { client ->
                val loader: coil.ImageLoader = retrieveLoader(client)
                val request: ImageRequest = ImageRequest.Builder(appContext)
                    .data(url)
                    .dispatcher(dispatchers.io)
                    .target(imageView)
                    .build()

                return DisposableAndroid(loader.enqueue(request))
            }
        }
    }

    private fun retrieveLoader(okHttpClient: OkHttpClient): coil.ImageLoader =
        loader ?: coil.ImageLoader.Builder(appContext)
            .okHttpClient(okHttpClient)
            .componentRegistry {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder())
                } else {
                    add(GifDecoder())
                }
                add(SvgDecoder(appContext))
                add(VideoFrameFileFetcher(appContext))
                add(VideoFrameUriFetcher(appContext))
            }
            .build()
            .also { loader = it }
}
