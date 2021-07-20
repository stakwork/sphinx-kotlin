package chat.sphinx.tribe_detail.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.tribe_detail.R
import chat.sphinx.tribe_detail.navigation.TribeDetailNavigator
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.dashboard.ChatId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val TribeDetailFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
internal class TribeDetailViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    chatRepository: ChatRepository,
    val navigator: TribeDetailNavigator,
): BaseViewModel<TribeDetailViewState>(dispatchers, TribeDetailViewState.Idle)
{
    private val args: TribeDetailFragmentArgs by savedStateHandle.navArgs()

    val chatId = args.chatId
    val podcast = args.argPodcast

    private val chatSharedFlow: SharedFlow<Chat?> = flow {
        emitAll(chatRepository.getChatById(chatId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    private suspend fun getChat(): Chat {
        chatSharedFlow.replayCache.firstOrNull()?.let { chat ->
            return chat
        }

        chatSharedFlow.firstOrNull()?.let { chat ->
            return chat
        }

        var chat: Chat? = null

        try {
            chatSharedFlow.collect {
                if (it != null) {
                    chat = it
                    throw Exception()
                }
            }
        } catch (e: Exception) {}
        delay(25L)

        return chat!!
    }

    fun load() {
        viewModelScope.launch(mainImmediate) {
            updateViewState(TribeDetailViewState.Tribe(getChat(), podcast))
        }
    }

    val imageLoaderDefaults by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_media_library)
            .transformation(Transformation.CircleCrop)
            .build()
    }
}
