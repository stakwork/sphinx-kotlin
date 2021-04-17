package chat.sphinx.chat_common

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    val dispatchers: CoroutineDispatchers,
): BaseViewModel<ChatViewState>(ChatViewState.Idle)
{
    @Suppress("RemoveExplicitTypeArguments")
    private val _chatDataStateFlow: MutableStateFlow<ChatData?> by lazy {
        MutableStateFlow<ChatData?>(null)
    }

    val chatDataStateFlow: StateFlow<ChatData?>
        get() = _chatDataStateFlow.asStateFlow()

    fun setChatData(chatData: ChatData) {
        _chatDataStateFlow.value = chatData
    }
}
