package chat.sphinx.onboard_connecting.ui

import android.content.Context
import chat.sphinx.onboard_resources.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class OnBoardConnectingSideEffect: SideEffect<Context>() {

    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = false
    ): OnBoardConnectingSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }

    object InvalidCode: OnBoardConnectingSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_invalid_code)
        }
    }

    object InputNullOrEmpty: OnBoardConnectingSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_empty_input)
        }
    }

    object InvalidPinLength: OnBoardConnectingSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_pin_length)
        }
    }

    object DecryptionFailure: OnBoardConnectingSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_decryption_failure)
        }
    }
    object InvalidPin: OnBoardConnectingSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_invalid_pin)
        }
    }

}