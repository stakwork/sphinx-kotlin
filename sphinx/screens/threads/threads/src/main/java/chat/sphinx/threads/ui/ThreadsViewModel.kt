package chat.sphinx.threads.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_common.ui.viewstate.messageholder.ReplyUserHolder
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.threads.R
import chat.sphinx.threads.model.ThreadItem
import chat.sphinx.threads.navigation.ThreadsNavigator
import chat.sphinx.threads.viewstate.ThreadsViewState
import chat.sphinx.wrapper_common.chatTimeFormat
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.message.toMessageUUID
import chat.sphinx.wrapper_common.timeAgo
import chat.sphinx.wrapper_contact.toContactAlias
import chat.sphinx.wrapper_message.ThreadUUID
import chat.sphinx.wrapper_message.getColorKey
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.absoluteValue

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

                messagesGrouped.mapNotNull { it.key?.value?.toMessageUUID() }.let { messageUUID ->
                    val headerMessage = messageRepository.getAllMessagesByUUID(messageUUID)
                    val headerMessageMappedByUUID = headerMessage.associateBy { it.uuid?.value }

                    val completeThreads = messagesGrouped.mapValues { groupEntry ->
                        val threadUUID = groupEntry.key
                        val threadMessages = groupEntry.value

                        val threadHeaderMessage = headerMessageMappedByUUID[threadUUID?.value]

                        if (threadHeaderMessage != null) {
                            listOf(threadHeaderMessage) + threadMessages
                        } else {
                            threadMessages
                        }
                    }

                    val threadItems = completeThreads.keys.map { uuid ->

                        val repliesList = completeThreads[uuid]?.drop(1)
                        val repliesExcess: Int? = if ((repliesList?.size ?: 0) > 6) repliesList?.size?.minus(6) else null

                        ThreadItem(
                            aliasAndColorKey = Pair(completeThreads[uuid]?.get(0)?.senderAlias?.value?.toContactAlias(), completeThreads[uuid]?.get(0)?.getColorKey()),
                            photoUrl =  completeThreads[uuid]?.get(0)?.senderPic,
                            date = completeThreads[uuid]?.get(0)?.date?.chatTimeFormat() ?: "",
                            message = completeThreads[uuid]?.get(0)?.messageContentDecrypted?.value ?: "",
                            usersReplies = repliesList?.take(6)?.map {
                                ReplyUserHolder(
                                    it.senderPic,
                                    it.senderAlias?.value?.toContactAlias(),
                                    it.getColorKey()
                                )
                            },
                            repliesAmount = String.format(app.getString(R.string.replies_amount) ,repliesList?.size?.toString() ?: "0"),
                            repliesExcess = String.format(app.getString(R.string.threads_plus), repliesExcess.toString()),
                            lastReplyDate = completeThreads[uuid]?.last()?.date?.timeAgo(),
                            uuid = uuid?.value ?: ""
                        )
                    }

                    updateViewState(ThreadsViewState.ThreadList(threadItems))
                }
            }
        }
    }

    fun navigateToThreadDetail(uuid: String) {
        viewModelScope.launch(mainImmediate) {
            navigator.toChatTribeThread(ChatId(args.argChatId), ThreadUUID(uuid))
        }
    }

}
