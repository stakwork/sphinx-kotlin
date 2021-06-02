package chat.sphinx.profile.ui

import android.content.Context
import chat.sphinx.profile.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class ProfileSideEffect: SideEffect<Context>() {

    object BackupKeysPinNeeded: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.profile_export_keys_pin_message)
        }
    }

    object BackupKeysExported: ProfileSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils().show(value, R.string.side_effect_backup_keys_exported)
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