package chat.sphinx.chat_common.ui

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.fragment.app.FragmentActivity
import chat.sphinx.chat_common.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

sealed class ChatSideEffect: SideEffect<ChatSideEffectFragment>() {

    object RetrieveImage: ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            try {
                value.contentChooserContract.launch("image/*")
            } catch (e: ActivityNotFoundException) {}
        }
    }

    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = true
    ): ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value.chatFragmentContext, msg)
        }
    }

    class CopyTextToClipboard(private val text: String): ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            copyToClipBoard(
                value,
                text,
                "text",
                value.chatFragmentContext.getString(R.string.side_effect_text_copied)
            )
        }
    }

    class CopyLinkToClipboard(private val link: String): ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            copyToClipBoard(
                value,
                link,
                "link",
                value.chatFragmentContext.getString(
                    R.string.side_effect_link_copied, link
                )
            )
        }
    }

    class CopyCallLinkToClipboard(private val link: String): ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            (value.chatFragmentContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { manager ->
                val clipData = ClipData.newPlainText("text", link)
                manager.setPrimaryClip(clipData)

                SphinxToastUtils().show(value.chatFragmentContext, R.string.side_effect_call_link_copied)
            }
        }
    }

    class AlertConfirmPayAttachment(
        private val callback: () -> Unit
    ): ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            val builder = AlertDialog.Builder(value.chatFragmentContext)
            builder.setTitle(value.chatFragmentContext.getString(R.string.alert_confirm_pay_attachment_title))
            builder.setMessage(value.chatFragmentContext.getString(R.string.alert_confirm_pay_attachment_message))
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

    object ProduceHapticFeedback: ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            value.chatFragmentWindow?.decorView?.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    companion object {
        fun copyToClipBoard(
            chatSideEffectFragment: ChatSideEffectFragment,
            contentToCopy: String,
            label: String,
            message: String
        ) {
            (chatSideEffectFragment.chatFragmentContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { manager ->
                val clipData = ClipData.newPlainText(label, contentToCopy)
                manager.setPrimaryClip(clipData)

                SphinxToastUtils().show(chatSideEffectFragment.chatFragmentContext, message)
            }
        }
    }
}
