package chat.sphinx.concept_image_loader

abstract class ImageLoader<ImageView> {
    abstract suspend fun load(imageView: ImageView, url: String)
}
