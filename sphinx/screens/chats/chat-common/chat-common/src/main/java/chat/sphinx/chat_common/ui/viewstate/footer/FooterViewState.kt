package chat.sphinx.chat_common.ui.viewstate.footer

import android.content.Context
import chat.sphinx.chat_common.R
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class FooterViewState: ViewState<FooterViewState>() {

    abstract val showMenuIcon: Boolean
    abstract val hintTextStringId: Int
    abstract val showSendIcon: Boolean
    open val showRecordAudioIcon: Boolean
        get() = !showSendIcon

    object Default: FooterViewState() {
        override val showMenuIcon: Boolean
            get() = true
        override val hintTextStringId: Int
            get() = R.string.edit_text_message_hint
        override val showSendIcon: Boolean
            get() = true
    }

    object Attachment: FooterViewState() {
        override val showMenuIcon: Boolean
            get() = false
        override val hintTextStringId: Int
            get() = R.string.edit_text_optional_message_hint
        override val showSendIcon: Boolean
            get() = true
    }

    object PendingApproval: FooterViewState() {
        override val showMenuIcon: Boolean
            get() = false
        override val hintTextStringId: Int
            get() = R.string.waiting_for_admin_approval
        override val showSendIcon: Boolean
            get() = false
        override val showRecordAudioIcon: Boolean
            get() = false
    }

    object MembershipRejected: FooterViewState() {
        override val showMenuIcon: Boolean
            get() = false
        override val hintTextStringId: Int
            get() = R.string.message
        override val showSendIcon: Boolean
            get() = false
        override val showRecordAudioIcon: Boolean
            get() = false
    }
}
