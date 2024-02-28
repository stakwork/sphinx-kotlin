package chat.sphinx.invite_friend.ui

import android.content.Context
import androidx.annotation.StringRes
import chat.sphinx.invite_friend.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class InviteFriendSideEffect: SideEffect<Context>() {

    sealed class Notify: InviteFriendSideEffect() {

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
    }

    object EmptyNickname : Notify() {
        override val stringRes: Int
            get() = R.string.invite_friend_nickname_empty
        override val showIcon: Boolean
            get() = false
        override val toastLengthLong: Boolean
            get() = true
    }

    object InviteFailed : Notify() {
        override val stringRes: Int
            get() = R.string.invite_failed
        override val showIcon: Boolean
            get() = false
        override val toastLengthLong: Boolean
            get() = true
    }

    object EmptySats : Notify() {
        override val stringRes: Int
            get() = R.string.invite_empty_sats
        override val showIcon: Boolean
            get() = false
        override val toastLengthLong: Boolean
            get() = true
    }
}