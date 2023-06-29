package chat.sphinx.dashboard.ui

import android.app.AlertDialog
import android.content.Context
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import chat.sphinx.dashboard.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

sealed class ChatListSideEffect: SideEffect<Context>() {

    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = false
    ): ChatListSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }

    class AlertConfirmPayInvite(
        private val amount: Long,
        private val callback: () -> Unit
    ): ChatListSideEffect() {
        override suspend fun execute(value: Context) {
            val successMessage = value.getString(R.string.alert_confirm_pay_invite_message, amount)

            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(value.getString(R.string.alert_confirm_pay_invite_title))
            builder.setMessage(successMessage)
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

    class AlertConfirmPayLightningPaymentRequest(
        private val amount: Long,
        private val memo: String = "",
        private val callback: () -> Unit
    ): ChatListSideEffect() {
        override suspend fun execute(value: Context) {
            val memo = if (memo.isEmpty()) "-" else memo
            val successMessage = value.getString(R.string.alert_confirm_pay_invoice_message, amount, memo)

            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(value.getString(R.string.alert_confirm_pay_invoice_title))
            builder.setMessage(successMessage)
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

    class AlertConfirmDeleteInvite(
        private val callback: () -> Unit
    ): ChatListSideEffect() {
        override suspend fun execute(value: Context) {
            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(value.getString(R.string.alert_confirm_delete_invite_title))
            builder.setMessage(value.getString(R.string.alert_confirm_delete_invite_message))
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

    class CheckNetwork(
        private val callback: () -> Unit,
    ): ChatListSideEffect() {
        override suspend fun execute(value: Context) {
            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(value.getString(R.string.network_check_title))
            builder.setMessage(value.getString(R.string.network_check_message))
            builder.setNegativeButton(R.string.no) { _, _ -> }
            builder.setPositiveButton(R.string.yes) { _, _ ->
                callback.invoke()
            }
            builder.show()
        }
    }

    class FailedToSetupSigningDevice(
        private val errorMessage: String
    ): ChatListSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(
                value,
                value.getString(R.string.error_setting_up_signing_device, errorMessage)
            )
        }
    }

    class SigningDeviceInfo(
        private val title: String,
        private val message: String,
        private val defaultValue: String? = null,
        private val inputType: Int? = null,
        private val callback: (String?) -> Unit,
    ): ChatListSideEffect() {

        override suspend fun execute(value: Context) {

            val inputEditTextField = EditText(value)
            inputEditTextField.isSingleLine = true
            inputType?.let {
                inputEditTextField.inputType = it
            }
            inputEditTextField.setOnFocusChangeListener { _, _ ->
                inputEditTextField.post {
                    val inputMethodManager: InputMethodManager =
                        value.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.showSoftInput(inputEditTextField, InputMethodManager.SHOW_IMPLICIT)
                }
            }
            inputEditTextField.requestFocus()
            inputEditTextField.setText(defaultValue)

            val container = FrameLayout(value)
            val params: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.leftMargin = value.resources.getDimensionPixelSize(R.dimen.default_layout_margin)
            params.rightMargin = value.resources.getDimensionPixelSize(R.dimen.default_layout_margin)
            inputEditTextField.layoutParams = params
            container.addView(inputEditTextField)

            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setView(container)
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                val editTextInput = inputEditTextField.text.toString()

                callback.invoke(
                    if (editTextInput.isEmpty()) {
                        null
                    } else {
                        editTextInput
                    }
                )
            }
            builder.setNegativeButton(android.R.string.cancel) { _, _ -> }

            builder.show()
        }
    }

    class CheckBitcoinNetwork(
        private val regTestCallback: () -> Unit,
        private val mainNetCallback: () -> Unit,
        private val callback: () -> Unit,
    ): ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(value.getString(R.string.select_bitcoin_network))

            val items = arrayOf(BITCOIN_NETWORK_REG_TEST.toCapitalized(), BITCOIN_NETWORK_MAIN_NET.toCapitalized())
            builder.setSingleChoiceItems(items, 0
            ) { _, p1 ->
                when (p1) {
                    0 -> {
                        regTestCallback.invoke()
                    }
                    1 -> {
                        mainNetCallback.invoke()
                    }
                    else -> {}
                }
            }
            builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback.invoke()
            }
            builder.show()
        }
    }

}