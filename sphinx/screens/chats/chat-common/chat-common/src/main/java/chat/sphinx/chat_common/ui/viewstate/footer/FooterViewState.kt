package chat.sphinx.chat_common.ui.viewstate.footer

import chat.sphinx.chat_common.R
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class FooterViewState: ViewState<FooterViewState>() {

    open val showMenuIcon: Boolean
        get() = true
    open val hintTextStringId: Int
        get() = R.string.edit_text_message_hint
    open val showSendIcon: Boolean
        get() = false
    open val showRecordAudioIcon: Boolean
        get() = !showSendIcon
    open val messagingEnabled: Boolean
        get() = true
    open val recordingEnabled: Boolean
        get() = false

    object Default: FooterViewState()

    object Attachment: FooterViewState() {
        override val showMenuIcon: Boolean
            get() = false
        override val showSendIcon: Boolean
            get() = true
        override val hintTextStringId: Int
            get() = R.string.edit_text_optional_message_hint
    }

    class RecordingAudioAttachment(val duration: Long): FooterViewState() {
        override val showMenuIcon: Boolean
            get() = false
        override val showSendIcon: Boolean
            get() = false
        override val recordingEnabled: Boolean
            get() = true
        override val messagingEnabled: Boolean
            get() = !recordingEnabled
    }

    object Disabled: FooterViewState() {
        override val messagingEnabled: Boolean
            get() = false
    }
}
