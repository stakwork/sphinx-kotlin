package chat.sphinx.feature_image_loader_android

import android.widget.ImageView
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_network_client_cache.NetworkClientCache

class ImageLoaderAndroid(
    private var networkClientCache: NetworkClientCache
): ImageLoader<ImageView>() {
    override suspend fun load(imageView: ImageView, url: String) {
        TODO("Not yet implemented")
    }
}
