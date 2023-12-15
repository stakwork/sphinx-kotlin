package chat.sphinx.profile.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import chat.sphinx.profile.R
import chat.sphinx.resources.R as R_common
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.wrapper_relay.RelayUrl
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect
import java.util.*

internal sealed class ProfileSideEffect: SideEffect<Context>() {

    class CopyBackupToClipboard(private val keys: String): ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(value.getString(R.string.profile_export_keys_title_alert))
            builder.setMessage(value.getString(R.string.profile_keys_copied_clipboard))
            builder.setPositiveButton(android.R.string.ok) { _, _ ->

                (value.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { manager ->
                    val clipData = ClipData.newPlainText("text", keys)
                    manager.setPrimaryClip(clipData)

                    SphinxToastUtils().show(value, R.string.side_effect_backup_keys_exported)
                }
            }

            builder.show()
        }
    }

    object BackupKeysPinNeeded: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.profile_export_keys_pin_message)
        }
    }

    object WrongPIN: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_wrong_pin)
        }
    }

    object BackupKeysFailed: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_backup_keys_failed)
        }
    }

    object FailedToProcessImage: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.failed_to_process_image)
        }
    }

    object ImageUpdatedSuccessfully: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.image_uploaded_successfully)
        }
    }

    class RelayUrlHttpConfirmation(
        private val relayUrl: RelayUrl,
        private val callback: (RelayUrl?) -> Unit,
    ): ProfileSideEffect() {

        override suspend fun execute(value: Context) {
            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(relayUrl.value)
            builder.setMessage(value.getString(R.string.relay_url_http_message))
            builder.setPositiveButton(R.string.relay_url_http_positive_change_to_https) { _, _ ->
                callback.invoke(RelayUrl(relayUrl.value.replaceFirst("http://", "https://")))
            }
            builder.setNegativeButton(R.string.relay_url_http_negative_keep_http) { _, _ ->
                callback.invoke(relayUrl)
            }
            builder.setOnCancelListener {
                callback.invoke(null)
            }

            builder.show()
        }
    }

    object UpdatingRelayUrl: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.testing_new_relay_url)
        }
    }

    object RelayUrlUpdateToTorNotSupported: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.relay_url_update_from_http_to_tor_not_implemented)
        }
    }

    object RelayUrlUpdatedSuccessfully: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.updating_relay_url_succeed)
        }
    }

    object FailedToUpdateRelayUrl: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.failed_updating_relay_url)
        }
    }

    object InvalidMeetingServerUrl: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.invalid_meeting_server_url)
        }
    }

    class GithubPATSet(
        private val callback: (String?) -> Unit,
    ): ProfileSideEffect() {

        override suspend fun execute(value: Context) {

            val inputEditTextField = EditText(value)
            inputEditTextField.isSingleLine = true
            inputEditTextField.setOnFocusChangeListener { _, _ ->
                inputEditTextField.post {
                    val inputMethodManager: InputMethodManager =
                        value.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.showSoftInput(inputEditTextField, InputMethodManager.SHOW_IMPLICIT)
                }
            }
            inputEditTextField.requestFocus()

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
            builder.setTitle(value.getString(R.string.github_pat_title))
            builder.setMessage(value.getString(R.string.github_pat_message))
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

    object GithubPATSuccessfullySet: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.github_pat_succeed)
        }
    }

    object FailedToSetGithubPat: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.github_pat_failed)
        }
    }

    class DeleteAccountConfirmation(
        private val confirmCallback: () -> Unit
    ): ProfileSideEffect() {

        override suspend fun execute(value: Context) {
            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(R.string.profile_delete_account)
            builder.setMessage(value.getString(R.string.profile_delete_account_description))
            builder.setPositiveButton(R_common.string.confirm) { _, _ ->
                confirmCallback()
            }
            builder.setOnCancelListener {}
            builder.show()
        }
    }

    object DeleteAccountError: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.profile_delete_account_error)
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
