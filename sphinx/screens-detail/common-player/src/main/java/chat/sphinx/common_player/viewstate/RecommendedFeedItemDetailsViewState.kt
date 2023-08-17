package chat.sphinx.common_player.viewstate

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.common_player.R
import chat.sphinx.wrapper_feed.FeedItemDetail
import io.matthewnelson.android_concept_views.MotionLayoutViewState

sealed class RecommendedFeedItemDetailsViewState: MotionLayoutViewState<RecommendedFeedItemDetailsViewState>() {

    object Closed: RecommendedFeedItemDetailsViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_feed_item_details_open
        override val endSetId: Int?
            get() = R.id.motion_scene_feed_item_details_closed

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    class Open(
        val feedItemDetail: FeedItemDetail?
    ) : RecommendedFeedItemDetailsViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_feed_item_details_closed
        override val endSetId: Int?
            get() = R.id.motion_scene_feed_item_details_open

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_feed_item_details_closed_to_open)
            motionLayout.setProgress(1F, 1F)
        }
    }
}