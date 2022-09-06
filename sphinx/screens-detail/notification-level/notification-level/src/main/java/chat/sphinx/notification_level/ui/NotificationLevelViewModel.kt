package chat.sphinx.notification_level.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.message
import chat.sphinx.notification_level.navigation.NotificationLevelNavigator
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.NotificationLevel
import chat.sphinx.wrapper_common.dashboard.ChatId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val NotificationLevelFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
internal class NotificationLevelViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    val navigator: NotificationLevelNavigator,
    private val chatRepository: ChatRepository
): SideEffectViewModel<
        Context,
        NotificationLevelSideEffect,
        NotificationLevelViewState>(
    dispatchers,
    NotificationLevelViewState.ChatNotificationLevel(NotificationLevel.SeeAll)
)
{
    private val args: NotificationLevelFragmentArgs by savedStateHandle.navArgs()

    val chatId = args.chatId

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

    init {
        viewModelScope.launch(mainImmediate) {
            chatSharedFlow.collect { chat ->
                updateViewState(NotificationLevelViewState.ChatNotificationLevel(chat?.notify))
            }
        }
    }

    private var toggleNotificationLevelJob: Job? = null
    fun selectNotificationLevel(level: NotificationLevel) {
        if (toggleNotificationLevelJob?.isActive == true) {
            return
        }

        toggleNotificationLevelJob = viewModelScope.launch(mainImmediate) {
            getChat()?.let { chat ->
                val response = chatRepository.setNotificationLevel(chat, level)

                if (response is Response.Error) {
                    submitSideEffect(NotificationLevelSideEffect.Notify(response.message))
                }
            }
        }
    }

}
