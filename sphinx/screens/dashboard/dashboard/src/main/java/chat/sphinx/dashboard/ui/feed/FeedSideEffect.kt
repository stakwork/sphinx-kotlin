package chat.sphinx.dashboard.ui.feed

import androidx.fragment.app.FragmentActivity
import chat.sphinx.dashboard.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

sealed class FeedSideEffect: SideEffect<FragmentActivity>() {
    object FailedToLoadFeed: FeedSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            SphinxToastUtils(false).show(value, value.getString(R.string.feed_search_result_load_error))
        }
    }
}