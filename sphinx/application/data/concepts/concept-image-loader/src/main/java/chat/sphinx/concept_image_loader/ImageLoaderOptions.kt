package chat.sphinx.concept_image_loader

class ImageLoaderOptions(
    val transformation: Transformation?,
    val transition: Transition,
    val errorResId: Int?,
    val placeholderResId: Int?,
) {

    class Builder {
        private var transformation: Transformation? = null
        private var transition: Transition = Transition.None
        private var errorResId: Int? = null
        private var placeholderResId: Int? = null

        fun transformation(transformation: Transformation) = apply {
            this.transformation = transformation
        }

        fun transition(transition: Transition) = apply {
            this.transition = transition
        }

        fun errorResId(resourceId: Int) = apply {
            this.errorResId = resourceId
        }

        fun placeholderResId(resourceId: Int) = apply {
            this.placeholderResId = resourceId
        }

        fun build() = ImageLoaderOptions(
            transformation,
            transition,
            errorResId,
            placeholderResId
        )
    }

}

sealed class Transformation {

    // TODO: Add support for:
    //  private val radius: Float = DEFAULT_RADIUS,
    //  private val sampling: Float = DEFAULT_SAMPLING
    object Blur: Transformation()

    object CircleCrop: Transformation()
    object GrayScale: Transformation()

    // TODO: Add support for:
    //  @Px private val topLeft: Float = 0f,
    //  @Px private val topRight: Float = 0f,
    //  @Px private val bottomLeft: Float = 0f,
    //  @Px private val bottomRight: Float = 0f
    object RoundedCorners: Transformation()
}

sealed class Transition {

    // TODO: Add support for:
    //  val durationMillis: Int = CrossfadeDrawable.DEFAULT_DURATION,
    //  val preferExactIntrinsicSize: Boolean = false
    object CrossFade: Transition()

    object None: Transition()
}