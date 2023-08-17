package chat.sphinx.common_player.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import chat.sphinx.common_player.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

    sealed class CommonPlayerScreenSideEffect: SideEffect<Context>() {

    sealed class Notify: CommonPlayerScreenSideEffect() {

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

        object ErrorPlayingClip : Notify() {
            override val stringRes: Int
                get() = R.string.error_playing_clip
        }

        object ErrorLoadingRecommendations : Notify() {
            override val stringRes: Int
                get() = R.string.error_loading_recommendations
        }

        object BalanceTooLow : Notify() {
            override val stringRes: Int
                get() = R.string.balance_too_low
        }

        object BoostAmountTooLow : Notify() {
            override val stringRes: Int
                get() = R.string.boost_amount_too_low
        }

        class CopyClipboardLink(override val stringRes: Int
        ): Notify() {
            override suspend fun execute(value: Context) {
                SphinxToastUtils(toastLengthLong = true).show(value, stringRes)
            }
        }

    }

}
