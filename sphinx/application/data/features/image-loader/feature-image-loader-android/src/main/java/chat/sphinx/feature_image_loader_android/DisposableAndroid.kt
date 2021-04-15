package chat.sphinx.feature_image_loader_android

import chat.sphinx.concept_image_loader.Disposable
import coil.annotation.ExperimentalCoilApi

class DisposableAndroid(private val disposable: coil.request.Disposable): Disposable() {
    override val isDisposed: Boolean
        get() = disposable.isDisposed

    override fun dispose() {
        disposable.dispose()
    }

    @OptIn(ExperimentalCoilApi::class)
    override suspend fun await() {
        disposable.await()
    }
}
