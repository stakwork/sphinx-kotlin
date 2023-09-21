package chat.sphinx.onboard_connect.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import chat.sphinx.onboard_connect.R
import chat.sphinx.onboard_connect.ui.OnBoardConnectViewModel.Companion.BITCOIN_NETWORK_MAIN_NET
import chat.sphinx.onboard_connect.ui.OnBoardConnectViewModel.Companion.BITCOIN_NETWORK_REG_TEST
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.wrapper_relay.RelayUrl
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect
import java.util.Locale

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

    class SigningDeviceInfo(
        private val title: String,
        private val message: String,
        private val defaultValue: String? = null,
        private val inputType: Int? = null,
        private val callback: (String?) -> Unit,
    ): OnBoardConnectSideEffect() {

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

    class CheckNetwork(
        private val callback: () -> Unit,
    ): OnBoardConnectSideEffect() {
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

    class ShowMnemonicToUser(
        private val mnemonic: String,
        private val callback: () -> Unit,
    ): OnBoardConnectSideEffect() {
        override suspend fun execute(value: Context) {
            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(value.getString(R.string.store_mnemonic))
            builder.setMessage(mnemonic)
            builder.setNeutralButton(android.R.string.copy) { _, _ ->
                (value.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { manager ->
                    val clipData = ClipData.newPlainText("mnemonic", mnemonic)
                    manager.setPrimaryClip(clipData)

                    SphinxToastUtils().show(value, R.string.mnemonic_copied_to_clipboard)
                }
                callback.invoke()
            }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback.invoke()
            }
            builder.show()
        }
    }

    class CheckBitcoinNetwork(
        private val regTestCallback: () -> Unit,
        private val mainNetCallback: () -> Unit,
        private val callback: () -> Unit,
    ): OnBoardConnectSideEffect() {
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

    object SendingSeedToHardware: OnBoardConnectSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.sending_seed)
        }
    }

    object SigningDeviceSuccessfullySet: OnBoardConnectSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.signing_device_successfully_set)
        }
    }

    class FailedToSetupSigningDevice(
        private val errorMessage: String
    ): OnBoardConnectSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(
                value,
                value.getString(R.string.error_setting_up_signing_device, errorMessage)
            )
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.toCapitalized(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(
            Locale.ROOT
        ) else it.toString()
    }
}
