package chat.sphinx.chat_common

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_contact.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(

): BaseViewModel<ChatViewState>(ChatViewState.Idle) {

    @Suppress("RemoveExplicitTypeArguments")
    private val _chatStateFlow: MutableStateFlow<Chat?> by lazy {
        MutableStateFlow<Chat?>(null)
    }

    val chatStateFlow: StateFlow<Chat?>
        get() = _chatStateFlow.asStateFlow()

    @Suppress("RemoveExplicitTypeArguments")
    private val _contactStateFlow: MutableStateFlow<Contact?> by lazy {
        MutableStateFlow<Contact?>(null)
    }

    val contactStateFlow: StateFlow<Contact?>
        get() = _contactStateFlow.asStateFlow()

    fun initializeChatData(chat: Chat?, contact: Contact?) {
        _chatStateFlow.value = chat
        _contactStateFlow.value = contact
    }
}
