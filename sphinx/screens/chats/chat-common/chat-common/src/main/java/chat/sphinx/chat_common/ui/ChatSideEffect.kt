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

    object RetrieveImageOrVideo: ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            try {
                value.contentChooserContract.launch(
                    arrayOf(
                        "image/*",
                        "video/*"
                    )
                )
            } catch (e: ActivityNotFoundException) {}
        }
    }

    object RetrieveFile: ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            try {
                value.contentChooserContract.launch(
                    arrayOf(
                        "application/*",
                        "text/*"
                    )
                )
            } catch (e: ActivityNotFoundException) {}
        }
    }

    class AlertConfirmDeleteContact(
        private val callback: () -> Unit
    ): ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            val successMessage = value.chatFragmentContext.getString(R.string.alert_confirm_delete_contact)

            val builder = AlertDialog.Builder(value.chatFragmentContext, R.style.AlertDialogTheme)
            builder.setTitle(value.chatFragmentContext.getString(R.string.alert_confirm_delete_contact_title))
            builder.setMessage(successMessage)
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }

    }

    class AlertConfirmToggleBlockContact(
        private val callback: () -> Unit,
        private val title: String,
        private val message: String,
    ): ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            val builder = AlertDialog.Builder(value.chatFragmentContext, R.style.AlertDialogTheme)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

    class AlertConfirmBlockContact(
        private val callback: () -> Unit,
    ): ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            val title = value.chatFragmentContext.getString(R.string.alert_confirm_block_contact_title)
            val message = value.chatFragmentContext.getString(R.string.alert_confirm_block_contact)

            AlertConfirmToggleBlockContact(callback, title, message).execute(value)
        }
    }

    class AlertConfirmUnblockContact(
        private val callback: () -> Unit,
    ): ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            val title = value.chatFragmentContext.getString(R.string.alert_confirm_unblock_contact_title)
            val message = value.chatFragmentContext.getString(R.string.alert_confirm_unblock_contact)

            AlertConfirmToggleBlockContact(callback, title, message).execute(value)
        }
    }

    object NotEncryptedContact : ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            SphinxToastUtils(toastLengthLong = false).show(
                value.chatFragmentContext,
                value.chatFragmentContext.getString(R.string.alert_not_encrypted_contact)
            )
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
            val builder = AlertDialog.Builder(value.chatFragmentContext, R.style.AlertDialogTheme)
            builder.setTitle(value.chatFragmentContext.getString(R.string.alert_confirm_pay_attachment_title))
            builder.setMessage(value.chatFragmentContext.getString(R.string.alert_confirm_pay_attachment_message))
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

    class AlertConfirmPayInvoice(
        private val callback: () -> Unit
    ): ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            val builder = AlertDialog.Builder(value.chatFragmentContext, R.style.AlertDialogTheme)
            builder.setTitle(value.chatFragmentContext.getString(R.string.alert_confirm_pay_chat_invoice_title))
            builder.setMessage(value.chatFragmentContext.getString(R.string.alert_confirm_pay_chat_invoice_message))
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }
    
    class AlertConfirmFlagMessage(
        private val callback: () -> Unit
    ): ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            val builder = AlertDialog.Builder(value.chatFragmentContext, R.style.AlertDialogTheme)
            builder.setTitle(value.chatFragmentContext.getString(R.string.alert_confirm_flag_message_title))
            builder.setMessage(value.chatFragmentContext.getString(R.string.alert_confirm_flag_message_message))
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

    class AlertConfirmDeleteMessage(
        private val callback: () -> Unit
    ): ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            val builder = AlertDialog.Builder(value.chatFragmentContext, R.style.AlertDialogTheme)
            builder.setTitle(value.chatFragmentContext.getString(R.string.alert_confirm_delete_message_title))
            builder.setMessage(value.chatFragmentContext.getString(R.string.alert_confirm_delete_message_message))
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

    class InsufficientBudget(private val text: String): ChatSideEffect() {
        override suspend fun execute(value: ChatSideEffectFragment) {
            copyToClipBoard(
                value,
                text,
                "text",
                value.chatFragmentContext.getString(R.string.side_effect_text_copied)
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
