package chat.sphinx.web_view.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.web_view.navigation.WebViewNavigator
import chat.sphinx.wrapper_common.dashboard.ChatId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val WebViewFragmentArgs.chatId: ChatId?
    get() = if (argChatId == ChatId.NULL_CHAT_ID.toLong()) {
        null
    } else {
        ChatId(argChatId)
    }

@HiltViewModel
internal class WebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    dispatchers: CoroutineDispatchers,
    val navigator: WebViewNavigator,
    private val chatRepository: ChatRepository,
): BaseViewModel<WebViewViewState>(dispatchers, WebViewViewState.Idle)
{
    private val args: WebViewFragmentArgs by savedStateHandle.navArgs()

    init {
        args.chatId?.let { chatId ->
            viewModelScope.launch(mainImmediate) {
                chatRepository.updateChatContentSeenAt(chatId)
            }
        }
    }
}
