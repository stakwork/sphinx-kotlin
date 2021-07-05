package chat.sphinx.chat_common.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import chat.sphinx.chat_common.R
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

    class CopyTextToClipboard(private val text: String): ChatSideEffect() {
        override suspend fun execute(value: Context) {
            (value.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { manager ->
                val clipData = ClipData.newPlainText("text", text)
                manager.setPrimaryClip(clipData)

                SphinxToastUtils().show(value, R.string.side_effect_text_copied)
            }
        }
    }
}
