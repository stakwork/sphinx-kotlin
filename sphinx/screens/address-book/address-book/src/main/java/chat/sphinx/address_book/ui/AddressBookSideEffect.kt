package chat.sphinx.address_book.ui

import android.app.AlertDialog
import android.content.Context
import androidx.fragment.app.FragmentActivity
import chat.sphinx.address_book.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class AddressBookSideEffect: SideEffect<FragmentActivity>() {
    class Notify(private val msg: String): AddressBookSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            SphinxToastUtils().show(value, msg)
        }
    }

    class AlertConfirmDeleteContact(
        private val callback: () -> Unit
    ): AddressBookSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            val successMessage = value.getString(R.string.alert_confirm_delete_contact)

            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(value.getString(R.string.alert_confirm_delete_contact_title))
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
    ): AddressBookSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
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
    ): AddressBookSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            val title = value.getString(R.string.alert_confirm_block_contact_title)
            val message = value.getString(R.string.alert_confirm_block_contact)

            AlertConfirmToggleBlockContact(callback, title, message).execute(value)
        }
    }

    class AlertConfirmUnblockContact(
        private val callback: () -> Unit,
    ): AddressBookSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            val title = value.getString(R.string.alert_confirm_unblock_contact_title)
            val message = value.getString(R.string.alert_confirm_unblock_contact)

            AlertConfirmToggleBlockContact(callback, title, message).execute(value)
        }
    }
}
