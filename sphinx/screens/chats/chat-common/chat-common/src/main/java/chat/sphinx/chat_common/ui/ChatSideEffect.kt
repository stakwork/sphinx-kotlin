package chat.sphinx.chat_common.ui

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

sealed class ChatSideEffect: SideEffect<Context>() {

    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = true
    ): ChatSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }
}
