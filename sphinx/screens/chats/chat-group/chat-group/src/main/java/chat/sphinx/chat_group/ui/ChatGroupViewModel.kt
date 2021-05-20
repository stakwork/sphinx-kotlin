package chat.sphinx.chat_group.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.wrapper_common.chat.ChatId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

internal inline val ChatGroupFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
class ChatGroupViewModel @Inject constructor(
    app: Application,
    dispatchers: CoroutineDispatchers,
    chatRepository: ChatRepository,
    messageRepository: MessageRepository,
    networkQueryLightning: NetworkQueryLightning,
    savedStateHandle: SavedStateHandle,
): ChatViewModel<ChatGroupFragmentArgs>(
    app,
    dispatchers,
    chatRepository,
    messageRepository,
    networkQueryLightning,
    savedStateHandle,
) {
    override val args: ChatGroupFragmentArgs by savedStateHandle.navArgs()
}
