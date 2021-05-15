package chat.sphinx.new_contact.ui

import android.content.Context
import androidx.annotation.StringRes
import chat.sphinx.new_contact.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class NewContactSideEffect: SideEffect<Context>() {

    @get:StringRes
    abstract val stringRes: Int

    override suspend fun execute(value: Context) {
        SphinxToastUtils().show(value, stringRes)
    }

    object PrivacyPinHelp: NewContactSideEffect() {
        override val stringRes: Int
            get() = R.string.new_contact_privacy_setting_help
    }

    object NicknameAndAddressRequired: NewContactSideEffect() {
        override val stringRes: Int
            get() = R.string.new_contact_nickname_address_empty
    }

    object InvalidLightningNodePublicKey: NewContactSideEffect() {
        override val stringRes: Int
            get() = R.string.new_contact_invalid_public_key_error
    }

    object InvalidRouteHint: NewContactSideEffect() {
        override val stringRes: Int
            get() = R.string.new_contact_invalid_route_hint_error
    }
}