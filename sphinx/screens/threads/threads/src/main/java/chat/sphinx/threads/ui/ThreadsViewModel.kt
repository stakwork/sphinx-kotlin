package chat.sphinx.threads.ui

import android.app.Application
import android.content.*
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_common.ui.viewstate.messageholder.ReplyUserHolder
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.threads.model.ThreadItem
import chat.sphinx.threads.navigation.ThreadsNavigator
import chat.sphinx.threads.viewstate.ThreadsViewState
import chat.sphinx.wrapper_common.chatTimeFormat
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.message.toMessageUUID
import chat.sphinx.wrapper_common.timeAgo
import chat.sphinx.wrapper_contact.toContactAlias
import chat.sphinx.wrapper_message.getColorKey
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ThreadsViewModel @Inject constructor(
    private val app: Application,
    val navigator: ThreadsNavigator,
    private val messageRepository: MessageRepository,
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        ThreadsSideEffect,
        ThreadsViewState
        >(dispatchers, ThreadsViewState.Idle)
{
    private val args: ThreadsFragmentArgs by savedStateHandle.navArgs()

    init {
        viewModelScope.launch(mainImmediate) {
            messageRepository.getThreadUUIDMessagesByChatId(ChatId(args.argChatId)).collect { threadUUIDMessages ->

                val messagesGrouped = threadUUIDMessages.groupBy { it.threadUUID }

                messagesGrouped.mapNotNull { it.key?.value?.toMessageUUID() }.let { messageUUIDs ->
                    val headerMessages = messageRepository.getAllMessagesByUUID(messageUUIDs)
                    val headerMessagesMappedByUUID = headerMessages.associateBy { it.uuid?.value }

                    val completeThreads = messagesGrouped.mapValues { groupEntry ->
                        val threadUUID = groupEntry.key
                        val threadMessages = groupEntry.value

                        val threadHeaderMessage = headerMessagesMappedByUUID[threadUUID?.value]

                        if (threadHeaderMessage != null) {
                            listOf(threadHeaderMessage) + threadMessages
                        } else {
                            threadMessages
                        }
                    }

                    val threadItems = completeThreads.keys.map { uuid ->
                        ThreadItem(
                            userName = completeThreads[uuid]?.get(0)?.senderAlias?.value ?: "",
                            userPic =  completeThreads[uuid]?.get(0)?.senderPic,
                            date = completeThreads[uuid]?.get(0)?.date?.chatTimeFormat() ?: "",
                            message = completeThreads[uuid]?.get(0)?.messageContentDecrypted?.value ?: "",
                            usersReplies = completeThreads[uuid]?.drop(1)?.map {
                                ReplyUserHolder(
                                    it.senderPic,
                                    it.senderAlias?.value?.toContactAlias(),
                                    it.getColorKey()
                                )
                            },
                            repliesAmount = completeThreads[uuid]?.drop(1)?.count().toString(),
                            repliesExcess = null,
                            lastReplyDate = completeThreads[uuid]?.last()?.date?.timeAgo()
                        )
                    }
                    updateViewState(ThreadsViewState.ThreadList(threadItems))
                }
            }
        }
    }

}
