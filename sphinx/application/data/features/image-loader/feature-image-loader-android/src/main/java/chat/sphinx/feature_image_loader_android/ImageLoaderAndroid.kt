package chat.sphinx.feature_image_loader_android

import android.widget.ImageView
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId

class ImageLoaderAndroid: ImageLoader<ImageView>() {
    override suspend fun load(imageView: ImageView, id: ChatId, url: String) {
        TODO("Not yet implemented")
    }

    override suspend fun load(imageView: ImageView, id: ContactId, url: String) {
        TODO("Not yet implemented")
    }
}
