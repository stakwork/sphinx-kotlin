package chat.sphinx.chat_common.ui.viewstate.footer

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class FooterViewState: ViewState<FooterViewState>() {

    abstract val showMenuIcon: Boolean
    abstract val hintText: String
    abstract val showSendIcon: Boolean
    val showRecordAudioIcon: Boolean
        get() = !showSendIcon

    object Default: FooterViewState() {
        override val showMenuIcon: Boolean
            get() = true
        override val hintText: String
            get() = "Message..."
        override val showSendIcon: Boolean
            get() = true
    }

    object Attachment: FooterViewState() {
        override val showMenuIcon: Boolean
            get() = false
        override val hintText: String
            get() = "Optional Message..."
        override val showSendIcon: Boolean
            get() = true
    }
}
