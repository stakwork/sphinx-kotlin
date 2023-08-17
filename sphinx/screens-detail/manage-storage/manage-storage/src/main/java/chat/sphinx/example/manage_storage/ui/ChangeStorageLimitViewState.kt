package chat.sphinx.example.manage_storage.ui

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.example.manage_storage.model.StorageLimit
import chat.sphinx.manage.storage.R
import chat.sphinx.wrapper_common.StorageData
import io.matthewnelson.android_concept_views.MotionLayoutViewState


sealed class ChangeStorageLimitViewState: MotionLayoutViewState<ChangeStorageLimitViewState>() {

    object Closed: ChangeStorageLimitViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_storage_limit_open
        override val endSetId: Int?
            get() = R.id.motion_scene_storage_limit_closed

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    data class Open(
        val storageLimit: StorageLimit
    ) : ChangeStorageLimitViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_storage_limit_closed
        override val endSetId: Int?
            get() = R.id.motion_scene_storage_limit_open

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_storage_limit_closed_to_open)
            motionLayout.setProgress(1F, 1F)
        }
    }
}