package chat.sphinx.splash.ui

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

sealed class SplashSideEffect: SideEffect<Context>() {
    object NotImplementedYet: SplashSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, "That feature is not yet implemented")
        }
    }
}