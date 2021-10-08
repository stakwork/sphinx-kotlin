package chat.sphinx.contact.ui

import android.content.Context
import androidx.annotation.StringRes
import chat.sphinx.contact.R
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

sealed class ContactSideEffect: SideEffect<Context>() {

    sealed class Notify: ContactSideEffect() {

        @get:StringRes
        abstract val stringRes: Int

        open val showIcon: Boolean
            get() = true
        open val toastLengthLong: Boolean
            get() = false

        override suspend fun execute(value: Context) {

            SphinxToastUtils(
                toastLengthLong = toastLengthLong,
                image = if (showIcon) SphinxToastUtils.DEFAULT_ICON else null
            ).show(value, stringRes)
        }

        object PrivacyPinHelp : Notify() {
            override val stringRes: Int
                get() = R.string.new_contact_privacy_setting_help
            override val showIcon: Boolean
                get() = false
            override val toastLengthLong: Boolean
                get() = true
        }

        object NicknameAndAddressRequired : Notify() {
            override val stringRes: Int
                get() = R.string.new_contact_nickname_address_empty
        }

        object InvalidLightningNodePublicKey : Notify() {
            override val stringRes: Int
                get() = R.string.new_contact_invalid_public_key_error
        }

        object InvalidRouteHint : Notify() {
            override val stringRes: Int
                get() = R.string.new_contact_invalid_route_hint_error
        }

        object FailedToSaveContact : Notify() {
            override val stringRes: Int
                get() = R.string.failed_to_update_contact
        }
    }

    data class ContactInfo(
        val pubKey: LightningNodePubKey,
        val routeHint: LightningRouteHint? = null
    ): ContactSideEffect() {
        override suspend fun execute(value: Context) {}
    }

    data class ExistingContact(
        val nickname: String?,
        val photoUrl: PhotoUrl?,
        val colorKey: String?,
        val pubKey: LightningNodePubKey,
        val routeHint: LightningRouteHint? = null,
        val subscribed: Boolean
    ): ContactSideEffect() {
        override suspend fun execute(value: Context) {}
    }
}