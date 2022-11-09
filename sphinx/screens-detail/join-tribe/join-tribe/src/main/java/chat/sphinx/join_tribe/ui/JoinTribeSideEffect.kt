package chat.sphinx.join_tribe.ui

import android.content.Context
import androidx.annotation.StringRes
import chat.sphinx.join_tribe.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class JoinTribeSideEffect: SideEffect<Context>() {

    sealed class Notify: JoinTribeSideEffect() {

        @get:StringRes
        abstract val stringRes: Int

        open val showIcon: Boolean
            get() = true
        open val toastLengthLong: Boolean
            get() = false

        override suspend fun execute(value: Context) {

            SphinxToastUtils(
                toastLengthLong = toastLengthLong,
                image = if (showIcon) SphinxToastUtils.DEFAULT_ICON else null
            ).show(value, stringRes)
        }

        object AliasRequired : Notify() {
            override val stringRes: Int
                get() = R.string.join_tribe_alias_empty
        }

        object InvalidTribe : Notify() {
            override val stringRes: Int
                get() = R.string.invalid_tribe
        }

        object ErrorJoining : Notify() {
            override val stringRes: Int
                get() = R.string.error_joining
        }

        object FailedToProcessImage: Notify() {
            override val stringRes: Int
                get() = R.string.failed_to_process_image
        }

        object AliasAllowedCharacters: Notify() {
            override val stringRes: Int
                get() = R.string.alias_allowed_characters
        }
    }
}