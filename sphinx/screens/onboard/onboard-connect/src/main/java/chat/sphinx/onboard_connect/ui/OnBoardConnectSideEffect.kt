package chat.sphinx.onboard_connect.ui

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class OnBoardConnectSideEffect: SideEffect<Context>() {

    data class FromScanner(val value: ScannerResponse): OnBoardConnectSideEffect() {
        override suspend fun execute(value: Context) {}
    }

    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = false
    ): OnBoardConnectSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }
}