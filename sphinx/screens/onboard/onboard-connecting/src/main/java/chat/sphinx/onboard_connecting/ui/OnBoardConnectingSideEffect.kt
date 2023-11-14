package chat.sphinx.onboard_connecting.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
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

    object GenerateTokenFailed: OnBoardConnectingSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_generate_token_failed)
        }
    }

    object InvalidCode: OnBoardConnectingSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_invalid_code)
        }
    }

    object InvalidInvite: OnBoardConnectingSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_invalid_invite)
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

    object CheckAdminFailed: OnBoardConnectingSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_check_admin_failed)
        }
    }

    class ShowMnemonicToUser(
        private val mnemonic: String,
        private val callback: () -> Unit,
    ): OnBoardConnectingSideEffect() {
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

}