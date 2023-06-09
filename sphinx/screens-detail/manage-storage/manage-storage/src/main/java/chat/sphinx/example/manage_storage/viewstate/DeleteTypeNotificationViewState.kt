package chat.sphinx.example.manage_storage.viewstate

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.manage.storage.R
import chat.sphinx.wrapper_feed.FeedItem
import io.matthewnelson.android_concept_views.MotionLayoutViewState


sealed class DeleteTypeNotificationViewState: MotionLayoutViewState<DeleteTypeNotificationViewState>() {

    object Closed: DeleteTypeNotificationViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_storage_delete_open
        override val endSetId: Int?
            get() = R.id.motion_scene_storage_delete_closed

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    class Open(
        val type: String,
    ): DeleteTypeNotificationViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_storage_delete_closed
        override val endSetId: Int?
            get() = R.id.motion_scene_storage_delete_open

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_tribe_app_closed_to_open)
            motionLayout.setProgress(1F, 1F)
        }
    }

}