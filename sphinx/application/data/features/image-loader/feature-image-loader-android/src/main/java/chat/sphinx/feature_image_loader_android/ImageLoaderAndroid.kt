package chat.sphinx.feature_image_loader_android

import android.content.Context
import android.os.Build
import android.widget.ImageView
import androidx.annotation.DrawableRes
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_image_loader.*
import chat.sphinx.concept_network_client.NetworkClientClearedListener
import chat.sphinx.concept_network_client_cache.NetworkClientCache
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import coil.annotation.ExperimentalCoilApi
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.fetch.VideoFrameFileFetcher
import coil.fetch.VideoFrameUriFetcher
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.transform.BlurTransformation
import coil.transform.CircleCropTransformation
import coil.transform.GrayscaleTransformation
import coil.transform.RoundedCornersTransformation
import coil.transition.CrossfadeTransition
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.io.File

class ImageLoaderAndroid(
    context: Context,
    private val dispatchers: CoroutineDispatchers,
    private val networkClientCache: NetworkClientCache,
    private val LOG: SphinxLogger,
) : ImageLoader<ImageView>(),
    NetworkClientClearedListener,
    CoroutineDispatchers by dispatchers
{

    companion object {
        const val TAG = "ImageLoaderAndroid"
    }

    private val appContext: Context = context.applicationContext

    @Volatile
    private var loader: coil.ImageLoader? = null
    private val loaderLock = Mutex()

    override fun networkClientCleared() {
        loader = null
    }

    init {
        networkClientCache.addListener(this)
    }

    override suspend fun load(
        imageView: ImageView,
        url: String,
        options: ImageLoaderOptions?,
        listener: OnImageLoadListener?,
    ): Disposable {
        return loadImpl(imageView, url, options, listener)
    }

    override suspend fun load(
        imageView: ImageView,
        @DrawableRes drawableResId: Int,
        options: ImageLoaderOptions?,
        listener: OnImageLoadListener?,
    ): Disposable {
        return loadImpl(imageView, drawableResId, options, listener)
    }

    override suspend fun load(
        imageView: ImageView,
        file: File,
        options: ImageLoaderOptions?,
        listener: OnImageLoadListener?,
    ): Disposable {
        return loadImpl(imageView, file, options, listener)
    }

    private suspend fun loadImpl(
        imageView: ImageView,
        any: Any,
        options: ImageLoaderOptions?,
        listener: OnImageLoadListener? = null,
    ): Disposable {
        loaderLock.withLock {
            val request = buildRequest(imageView, any, options, listener)

            // Future-proofing:
            // Always retrieve the client, as Tor may be enabled but
            // in a suspended state and we don't want to do any requests
            // w/o a proxied client.
            val client = networkClientCache.getCachingClient()
            val loader: coil.ImageLoader = retrieveLoader(client)

            return DisposableAndroid(loader.enqueue(request.build()))
        }
    }

    private suspend fun loadImmediateImpl(
        imageView: ImageView,
        any: Any,
        options: ImageLoaderOptions?,
        listener: OnImageLoadListener? = null,
    ) {
        loaderLock.withLock {
            val client = networkClientCache.getCachingClient()
            retrieveLoader(client)
        }.let { loader ->
            val builder = buildRequest(imageView, any, options, listener)
            loader.execute(builder.build())
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun buildRequest(
        imageView: ImageView,
        any: Any,
        options: ImageLoaderOptions?,
        listener: OnImageLoadListener? = null,
    ): ImageRequest.Builder {
        val request = ImageRequest.Builder(appContext)
            .data(any)
            .dispatcher(io)
            .listener(
                object: ImageRequest.Listener {
                    override fun onSuccess(request: ImageRequest, metadata: ImageResult.Metadata) {
                        super.onSuccess(request, metadata)
                        listener?.let {
                            it.onSuccess()
                        }
                    }
                }
            )
            .target(imageView)

        options?.let {
            it.errorResId?.let { errorRes ->
                request.error(errorRes)
            }
            it.placeholderResId?.let { placeholderRes ->
                request.placeholder(placeholderRes)
            }
            it.transformation?.let { transform ->
                @Exhaustive
                when (transform) {
                    is Transformation.Blur -> {
                        request.transformations(
                            BlurTransformation(
                                appContext,
                                transform.radius,
                                transform.sampling
                            )
                        )
                    }
                    is Transformation.CircleCrop -> {
                        request.transformations(
                            CircleCropTransformation()
                        )
                    }
                    is Transformation.GrayScale -> {
                        request.transformations(
                            GrayscaleTransformation()
                        )
                    }
                    is Transformation.RoundedCorners -> {
                        request.transformations(
                            RoundedCornersTransformation(
                                transform.topLeft.value,
                                transform.topRight.value,
                                transform.bottomLeft.value,
                                transform.bottomRight.value
                            )
                        )
                    }
                }
            }

            it.transition.let { transition ->
                @Exhaustive
                when (transition) {
                    is Transition.CrossFade -> {
                        request.transition(
                            CrossfadeTransition(
                                transition.durationMillis,
                                transition.preferExactIntrinsicSize
                            )
                        )
                    }
                    is Transition.None -> {
                        request.transition(
                            coil.transition.Transition.NONE
                        )
                    }
                }
            }

            for (entry in it.additionalHeaders.entries) {
                try {
                    request.addHeader(entry.key, entry.value)
                } catch (e: Exception) {
                    LOG.e(TAG, "Failed to add header to request", e)
                }
            }
        }

        return request
    }

    private fun retrieveLoader(okHttpClient: OkHttpClient): coil.ImageLoader =
        loader ?: coil.ImageLoader.Builder(appContext)
            .okHttpClient(okHttpClient)
            .componentRegistry {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder(appContext))
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
