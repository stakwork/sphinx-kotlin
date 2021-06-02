package chat.sphinx.splash.ui

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.splash.R
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class SplashSideEffect: SideEffect<Context>() {
    object GenerateTokenFailed: SplashSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_generate_token_failed)
        }
    }
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
    object InvalidPinLength: SplashSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_pin_length)
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
    object InvalidPin: SplashSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_invalid_pin)
        }
    }

    data class FromScanner(val value: ScannerResponse): SplashSideEffect() {
        override suspend fun execute(value: Context) {}
    }
}
