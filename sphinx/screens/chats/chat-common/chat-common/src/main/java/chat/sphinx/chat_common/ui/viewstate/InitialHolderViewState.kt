package chat.sphinx.chat_common.ui.viewstate

import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.databinding.LayoutMessageStatusHeaderBinding
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.resources.setInitialsColor
import chat.sphinx.wrapper_common.PhotoUrl
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible

/*
* TODO: Separate out logic for setting views into extension function(s)
* */
sealed class InitialHolderViewState {

    abstract suspend fun setInitialHolder(
        textViewInitials: TextView,
        imageViewPicture: ImageView,
        statusHeader: LayoutMessageStatusHeaderBinding,
        imageLoader: ImageLoader<ImageView>,
        @ColorInt color: Int? = null,
    ): Disposable?

    object None: InitialHolderViewState() {
        override suspend fun setInitialHolder(
            textViewInitials: TextView,
            imageViewPicture: ImageView,
            statusHeader: LayoutMessageStatusHeaderBinding,
            imageLoader: ImageLoader<ImageView>,
            @ColorInt color: Int?,
        ): Disposable? {
            textViewInitials.gone
            imageViewPicture.gone

            return null
        }
    }

    data class Initials(
        val initials: String,
        val colorKey: String,
    ): InitialHolderViewState() {
        override suspend fun setInitialHolder(
            textViewInitials: TextView,
            imageViewPicture: ImageView,
            statusHeader: LayoutMessageStatusHeaderBinding,
            imageLoader: ImageLoader<ImageView>,
            @ColorInt color: Int?,
        ): Disposable? {
            textViewInitials.visible
            imageViewPicture.gone

            textViewInitials.text = initials

            textViewInitials.setInitialsColor(
                color,
                R.drawable.chat_initials_circle
            )
            return null
        }
    }

    data class Url(val photoUrl: PhotoUrl): InitialHolderViewState() {
        override suspend fun setInitialHolder(
            textViewInitials: TextView,
            imageViewPicture: ImageView,
            statusHeader: LayoutMessageStatusHeaderBinding,
            imageLoader: ImageLoader<ImageView>,
            @ColorInt color: Int?,
        ): Disposable {
            textViewInitials.gone
            imageViewPicture.visible

            return imageLoader.load(
                imageViewPicture,
                photoUrl.value,
                ImageLoaderOptions.Builder()
                    .placeholderResId(R.drawable.ic_profile_avatar_circle)
                    .transformation(Transformation.CircleCrop)
                    .build()
            )
        }
    }
}
