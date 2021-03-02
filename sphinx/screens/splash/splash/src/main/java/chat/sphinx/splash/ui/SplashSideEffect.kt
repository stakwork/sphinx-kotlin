package chat.sphinx.splash.ui

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.splash.R
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class SplashSideEffect: SideEffect<Context>() {
    object NotImplementedYet: SplashSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_feature_not_implemented)
        }
    }
    object InputNullOrEmpty: SplashSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_empty_input)
        }
    }
    object InvalidCode: SplashSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_invalid_code)
        }
    }
    object DecryptionFailure: SplashSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_decryption_failure)
        }

    }
}
