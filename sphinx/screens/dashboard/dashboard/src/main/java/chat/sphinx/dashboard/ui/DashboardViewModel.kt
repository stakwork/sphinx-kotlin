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
import chat.sphinx.dashboard.ui.viewstates.NavDrawerViewState
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_message.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.sideeffect.SideEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

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

    private inner class DashboardChatState {
        private val _chatsStateFlow: MutableStateFlow<List<DashboardChat>> by lazy {
            MutableStateFlow(emptyList())
        }

        private val lock = Mutex()
        suspend fun updateList(list: ArrayList<DashboardChat>) {
            lock.withLock {
                _chatsStateFlow.value = withContext(dispatchers.default) {

                    // TODO: update sorting to prefer:
                    //  1st: message.date.time
                    //  2nd: contact.createdAt.time
                    list.sortedByDescending {
                        it.chat.latestMessageId?.value
                    }
                }
            }
        }

        val chatsStateFlow: StateFlow<List<DashboardChat>>
            get() = _chatsStateFlow.asStateFlow()
    }

    private val dashboardChatState: DashboardChatState by lazy {
        DashboardChatState()
    }

    val chatsStateFlow: StateFlow<List<DashboardChat>>
        get() = dashboardChatState.chatsStateFlow

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

                dashboardChatState.updateList(newList)
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
