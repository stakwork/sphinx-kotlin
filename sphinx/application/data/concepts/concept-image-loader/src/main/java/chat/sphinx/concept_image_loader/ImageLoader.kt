package chat.sphinx.concept_image_loader

abstract class ImageLoader<ImageView> {
    abstract suspend fun load(imageView: ImageView, url: String): Disposable
    abstract suspend fun load(imageView: ImageView, drawableResId: Int): Disposable
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
