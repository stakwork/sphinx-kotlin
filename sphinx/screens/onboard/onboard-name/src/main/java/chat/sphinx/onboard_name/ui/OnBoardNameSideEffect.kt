package chat.sphinx.onboard_name.ui

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.onboard_name.R
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class OnBoardNameSideEffect: SideEffect<Context>() {
    object GetContactsFailed: OnBoardNameSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_get_contacts_failed)
        }
    }

    object UpdateOwnerFailed: OnBoardNameSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_update_owner_failed)
        }
    }
}
