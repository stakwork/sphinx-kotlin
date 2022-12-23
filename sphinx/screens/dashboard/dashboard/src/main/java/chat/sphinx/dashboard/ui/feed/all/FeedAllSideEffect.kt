package chat.sphinx.dashboard.ui.feed.all

import androidx.fragment.app.FragmentActivity
import chat.sphinx.dashboard.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

sealed class FeedAllSideEffect: SideEffect<FragmentActivity>() {

    object RefreshWhilePlaying: FeedAllSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            SphinxToastUtils(false).show(value, value.getString(R.string.refresh_while_loading))
        }
    }
}