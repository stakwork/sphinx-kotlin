package chat.sphinx.feature_image_loader_android

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.DrawableRes
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
        return loadImpl(imageView, url)
    }

    override suspend fun load(imageView: ImageView, @DrawableRes drawableResId: Int): Disposable {
        return loadImpl(imageView, drawableResId)
    }

    private suspend fun loadImpl(imageView: ImageView, any: Any): Disposable {
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
                val request: ImageRequest.Builder = ImageRequest.Builder(appContext)
                    .data(any)
                    .dispatcher(dispatchers.io)
                    .target(imageView)

                return DisposableAndroid(loader.enqueue(request.build()))
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
