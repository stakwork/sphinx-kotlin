package chat.sphinx.dashboard.ui.feed.all

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

sealed class FeedAllSideEffect: SideEffect<Context>() {

    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = true
    ): FeedAllSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }
}