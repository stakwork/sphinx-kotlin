package chat.sphinx.leaderboard.ui

import androidx.fragment.app.FragmentActivity
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

sealed class LeaderboardSideEffect: SideEffect<FragmentActivity>() {

    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = true
    ): LeaderboardSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }
}
