package chat.sphinx.feature_image_loader_android

import android.content.Context
import android.os.Build
import android.widget.ImageView
import androidx.annotation.DrawableRes
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_image_loader.*
import chat.sphinx.concept_network_client_cache.NetworkClientCache
import coil.annotation.ExperimentalCoilApi
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.fetch.VideoFrameFileFetcher
import coil.fetch.VideoFrameUriFetcher
import coil.transform.BlurTransformation
import coil.transform.CircleCropTransformation
import coil.transform.GrayscaleTransformation
import coil.transform.RoundedCornersTransformation
import coil.transition.CrossfadeTransition
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

    init {
        networkClientCache.addOnClientClearedCallback {
            loader = null
        }
    }

    override suspend fun load(
        imageView: ImageView,
        url: String,
        options: ImageLoaderOptions?
    ): Disposable {
        return loadImpl(imageView, url, options)
    }

    override suspend fun load(
        imageView: ImageView,
        @DrawableRes drawableResId: Int,
        options: ImageLoaderOptions?
    ): Disposable {
        return loadImpl(imageView, drawableResId, options)
    }

    @OptIn(ExperimentalCoilApi::class)
    private suspend fun loadImpl(
        imageView: ImageView,
        any: Any,
        options: ImageLoaderOptions?
    ): Disposable {
        loaderLock.withLock {
            val request = coil.request.ImageRequest.Builder(appContext)
                .data(any)
                .dispatcher(dispatchers.io)
                .target(imageView)

            request.apply {
                options?.errorResId?.let {
                    error(it)
                }
                options?.placeholderResId?.let {
                    placeholder(it)
                }
                options?.transformation?.let {
                    @Exhaustive
                    when (it) {
                        Transformation.Blur -> {
                            transformations(BlurTransformation(appContext))
                        }
                        Transformation.CircleCrop -> {
                            transformations(CircleCropTransformation())
                        }
                        Transformation.GrayScale -> {
                            transformations(GrayscaleTransformation())
                        }
                        Transformation.RoundedCorners -> {
                            transformations(RoundedCornersTransformation())
                        }
                    }
                }
                if (options?.transition is Transition.CrossFade) {
                    request.transition(CrossfadeTransition())
                }
            }

            // Future-proofing:
            // Always retrieve the client, as Tor may be enabled but
            // in a suspended state and we don't want to do any requests
            // w/o a proxied client.
            val client = networkClientCache.getCachingClient()
            val loader: coil.ImageLoader = retrieveLoader(client)

            return DisposableAndroid(loader.enqueue(request.build()))
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
