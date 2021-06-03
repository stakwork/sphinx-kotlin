package chat.sphinx.onboard_ready.ui

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.onboard_ready.R
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class OnBoardReadySideEffect: SideEffect<Context>() {
    object CreateInviterFailed: OnBoardReadySideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_create_inviter_failed)
        }
    }
}
