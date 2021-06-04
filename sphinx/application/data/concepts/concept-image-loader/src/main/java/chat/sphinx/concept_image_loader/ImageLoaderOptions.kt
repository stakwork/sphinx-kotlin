package chat.sphinx.concept_image_loader

import chat.sphinx.wrapper_view.Px

class ImageLoaderOptions private constructor(
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

    class Blur(
        val radius: Float = DEFAULT_RADIUS,
        val sampling: Float = DEFAULT_SAMPLING,
    ): Transformation() {
        companion object {
            const val DEFAULT_RADIUS = 10f
            const val DEFAULT_SAMPLING = 1f
        }
    }

    object CircleCrop: Transformation()
    object GrayScale: Transformation()

    class RoundedCorners(
        val topLeft: Px = Px(0f),
        val topRight: Px = Px(0f),
        val bottomLeft: Px = Px(0f),
        val bottomRight: Px = Px(0f),
    ): Transformation()
}

sealed class Transition {

    class CrossFade(
        val durationMillis: Int = DEFAULT_DURATION,
        val preferExactIntrinsicSize: Boolean = false
    ): Transition() {
        companion object {
            const val DEFAULT_DURATION = 100
        }
    }

    object None: Transition()
}