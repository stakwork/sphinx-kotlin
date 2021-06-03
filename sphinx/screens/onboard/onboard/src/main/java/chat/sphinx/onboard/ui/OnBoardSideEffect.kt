package chat.sphinx.onboard.ui

import android.content.Context
import chat.sphinx.onboard.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class OnBoardSideEffect: SideEffect<Context>() {
    object GenerateTokenFailed: OnBoardSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_generate_token_failed)
        }
    }
}
