package chat.sphinx.concept_image_loader

import java.io.File

abstract class ImageLoader<ImageView> {

    abstract suspend fun load(
        imageView: ImageView,
        url: String,
        options: ImageLoaderOptions? = null,
        listener: OnImageLoadListener? = null,
    ): Disposable

    abstract suspend fun load(
        imageView: ImageView,
        drawableResId: Int,
        options: ImageLoaderOptions? = null,
        listener: OnImageLoadListener? = null,
    ): Disposable

    abstract suspend fun load(
        imageView: ImageView,
        file: File,
        options: ImageLoaderOptions? = null,
        listener: OnImageLoadListener? = null,
    ): Disposable

}

interface OnImageLoadListener {
    fun onSuccess() {}
}

/**
 * Kotlin wrapper for Coil Android Library.
 * */
abstract class Disposable {

    /**
     * Returns true if the request is complete or cancelling.
     */
    abstract val isDisposed: Boolean

    /**
     * Cancels any in progress work and frees any resources associated with this request. This method is idempotent.
     */
    abstract fun dispose()

    /**
     * Suspends until any in progress work completes.
     */
    abstract suspend fun await()
}
