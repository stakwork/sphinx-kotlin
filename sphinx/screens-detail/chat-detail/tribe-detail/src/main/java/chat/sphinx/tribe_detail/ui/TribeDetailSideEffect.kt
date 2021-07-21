package chat.sphinx.tribe_detail.ui

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.tribe_detail.R
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class  TribeDetailSideEffect: SideEffect<Context>()  {

    object FailedToUpdateProfileAlias: TribeDetailSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(false).show(value, R.string.failed_to_update_profile_alias)
        }
    }

    object FailedToUpdateProfilePic: TribeDetailSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(false).show(value, R.string.failed_to_update_profile_pic)
        }
    }
}