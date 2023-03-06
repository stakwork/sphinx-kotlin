package chat.sphinx.create_badge.ui

import android.content.Context
import androidx.annotation.StringRes
import chat.sphinx.create_badge.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class CreateBadgeSideEffect : SideEffect<Context>()  {

    sealed class Notify: CreateBadgeSideEffect() {

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

        object FailedToCreateBadge : Notify() {
            override val stringRes: Int
                get() = R.string.badges_create_error
        }

        object FailedToChangeState : Notify() {
            override val stringRes: Int
                get() = R.string.badges_state_error
        }
    }

}
