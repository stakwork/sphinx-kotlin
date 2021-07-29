package chat.sphinx.chat_common.ui.viewstate.footer

import chat.sphinx.chat_common.R
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class FooterViewState: ViewState<FooterViewState>() {

    open val showMenuIcon: Boolean
        get() = true
    open val hintTextStringId: Int
        get() = R.string.edit_text_message_hint
    open val showSendIcon: Boolean
        get() = true
    open val showRecordAudioIcon: Boolean
        get() = !showSendIcon
    open val enableMessaging: Boolean
        get() = true

    object Default: FooterViewState()

    object Attachment: FooterViewState() {
        override val showMenuIcon: Boolean
            get() = false
        override val hintTextStringId: Int
            get() = R.string.edit_text_optional_message_hint
    }

    object PendingApproval: FooterViewState() {
        override val enableMessaging: Boolean
            get() = false
    }
}
