package chat.sphinx.concept_image_loader

import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId

abstract class ImageLoader<ImageView> {
    abstract suspend fun load(imageView: ImageView, id: ChatId, url: String)

    abstract suspend fun load(imageView: ImageView, id: ContactId, url: String)
}
