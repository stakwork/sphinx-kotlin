package chat.sphinx.dashboard.ui

import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.dashboard.navigation.DashboardBottomNavBarNavigator
import chat.sphinx.dashboard.navigation.DashboardNavDrawerNavigator
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.adapter.DashboardChat
import chat.sphinx.dashboard.ui.viewstates.ChatViewState
import chat.sphinx.dashboard.ui.viewstates.ChatViewStateContainer
import chat.sphinx.dashboard.ui.viewstates.NavDrawerViewState
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_message.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.sideeffect.SideEffect
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal suspend inline fun DashboardViewModel.collectChatViewState(
    crossinline action: suspend (value: ChatViewState) -> Unit
): Unit =
    chatViewStateContainer.collect { action(it) }

internal val DashboardViewModel.currentChatViewState: ChatViewState
    get() = chatViewStateContainer.value

internal suspend inline fun DashboardViewModel.updateSearchParams(filter: String) {
    chatViewStateContainer.updateDashboardChats(currentChatViewState.list, filter)
}

@HiltViewModel
internal class DashboardViewModel @Inject constructor(
    val dashboardNavigator: DashboardNavigator,
    val navBarNavigator: DashboardBottomNavBarNavigator,
    val navDrawerNavigator: DashboardNavDrawerNavigator,

    val dispatchers: CoroutineDispatchers,
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
): MotionLayoutViewModel<
        Any,
        Nothing,
        SideEffect<Nothing>,
        NavDrawerViewState
        >(NavDrawerViewState.Closed)
{

    val chatViewStateContainer: ChatViewStateContainer by lazy {
        ChatViewStateContainer(dispatchers)
    }

    init {
        viewModelScope.launch(dispatchers.mainImmediate) {
            chatRepository.getChats().distinctUntilChanged().collect { chats ->
                val newList = ArrayList<DashboardChat>(chats.size)

                withContext(dispatchers.default) {
                    for (chat in chats) {
                        val message: Message? = chat.latestMessageId?.let {
                            // TODO: Fix DB's messages table
//                        messageRepository.getLatestMessageForChat(chat.id).firstOrNull()
                            null
                        }

                        newList.add(DashboardChat(chat, message))
                    }
                }

                chatViewStateContainer.updateDashboardChats(newList)
            }
        }
    }

    private val _networkStateFlow: MutableStateFlow<LoadResponse<Boolean, ResponseError>> by lazy {
        MutableStateFlow(LoadResponse.Loading)
    }

    val networkStateFlow: StateFlow<LoadResponse<Boolean, ResponseError>>
        get() = _networkStateFlow.asStateFlow()

    private var jobNetworkRefresh: Job? = null
    fun networkRefresh() {
        if (jobNetworkRefresh?.isActive == true) {
            return
        }

        jobNetworkRefresh = viewModelScope.launch(dispatchers.mainImmediate) {
            contactRepository.networkRefreshContacts().collect { response ->
                @Exhaustive
                when (response) {
                    is LoadResponse.Loading,
                    is Response.Error -> {
                        _networkStateFlow.value = response
                    }
                    is Response.Success -> {}
                }
            }

            if (_networkStateFlow.value is Response.Error) {
                jobNetworkRefresh?.cancel()
            }

            messageRepository.networkRefreshMessages().collect { response ->
                _networkStateFlow.value = response
            }
        }
    }

    override suspend fun onMotionSceneCompletion(value: Any) {
        // Unused
    }
}
