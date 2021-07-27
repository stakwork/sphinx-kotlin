package chat.sphinx.onboard_picture.ui

import android.content.Context
import chat.sphinx.onboard_picture.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class OnBoardPictureSideEffect: SideEffect<Context>() {
    object NotifyUploadError: OnBoardPictureSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.on_board_picture_side_effect_upload_failure)
        }
    }
}
