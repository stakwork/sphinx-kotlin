package chat.sphinx.profile.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import chat.sphinx.profile.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class ProfileSideEffect: SideEffect<Context>() {

    class CopyBackupToClipboard(private val keys: String): ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            val builder = AlertDialog.Builder(value)
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
}