package chat.sphinx.threads.ui

import android.app.Application
import android.content.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_common.ui.viewstate.messageholder.ReplyUserHolder
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.highlighting_tool.highlightedTexts
import chat.sphinx.highlighting_tool.replacingHighlightedDelimiters
import chat.sphinx.threads.R
import chat.sphinx.threads.model.FileAttachment
import chat.sphinx.threads.model.ThreadItem
import chat.sphinx.threads.navigation.ThreadsNavigator
import chat.sphinx.threads.viewstate.ThreadsViewState
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.FileSize
import chat.sphinx.wrapper_common.chatTimeFormat
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.message.toMessageUUID
import chat.sphinx.wrapper_common.timeAgo
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.getColorKey
import chat.sphinx.wrapper_contact.toContactAlias
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.ThreadUUID
import chat.sphinx.wrapper_message.getColorKey
import chat.sphinx.wrapper_message.retrieveImageUrlAndMessageMedia
import chat.sphinx.wrapper_message_media.isAudio
import chat.sphinx.wrapper_message_media.isImage
import chat.sphinx.wrapper_message_media.isPdf
import chat.sphinx.wrapper_message_media.isUnknown
import chat.sphinx.wrapper_message_media.isVideo
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class ThreadsViewModel @Inject constructor(
    private val app: Application,
    val navigator: ThreadsNavigator,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val chatRepository: ChatRepository,
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        ThreadsSideEffect,
        ThreadsViewState
        >(dispatchers, ThreadsViewState.Idle)
{
    private val args: ThreadsFragmentArgs by savedStateHandle.navArgs()

    private val _ownerStateFlow: MutableStateFlow<Contact?> by lazy {
        MutableStateFlow(null)
    }
    private val ownerStateFlow: StateFlow<Contact?>?
        get() = _ownerStateFlow.asStateFlow()

    private val chatSharedFlow: SharedFlow<Chat?> = flow {
        emitAll(chatRepository.getChatById(ChatId(args.argChatId)))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    private suspend fun getOwner(): Contact {
        return contactRepository.accountOwner.value.let { contact ->
            if (contact != null) {
                contact
            } else {
                var resolvedOwner: Contact? = null
                try {
                    contactRepository.accountOwner.collect { ownerContact ->
                        if (ownerContact != null) {
                            resolvedOwner = ownerContact
                            throw Exception()
                        }
                    }
                } catch (e: Exception) {
                }
                delay(25L)

                resolvedOwner!!
            }
        }
    }

    suspend fun getChat(): Chat {
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
        } catch (e: Exception) {
        }
        delay(25L)

        return chat!!
    }

    init {
        initializeOwner()
        updateThreads()
    }

    private fun initializeOwner() {
        viewModelScope.launch(mainImmediate) {
            _ownerStateFlow.value = getOwner()
        }
    }

    private fun updateThreads() {
        viewModelScope.launch(mainImmediate) {
            messageRepository.getThreadUUIDMessagesByChatId(ChatId(args.argChatId)).collect { messages ->

                val threadItems = generateThreadItemsList(messages)

                updateViewState(ThreadsViewState.ThreadList(threadItems))
            }
        }
    }

    private suspend fun generateThreadItemsList(messages: List<Message>): List<ThreadItem> {
        // Group messages by their ThreadUUID
        val groupedMessagesByThread = messages.groupBy { it.threadUUID }.filter {
            it.value.size > 1
        }

        // Fetch the header messages based on the message UUIDs
        val headerMessages = messageRepository.getAllMessagesByUUID(groupedMessagesByThread.keys.mapNotNull { it?.value?.toMessageUUID() })
        val headerMessagesMappedByUUID = headerMessages.associateBy { it.uuid?.value }

        // Generate a map of complete threads, where each thread includes its header message and its other messages
        val completeThreads = groupedMessagesByThread.mapValues { entry ->
            val threadUUID = entry.key
            val threadMessages = entry.value

            val threadHeaderMessage = headerMessagesMappedByUUID[threadUUID?.value]

            if (threadHeaderMessage != null) {
                listOf(threadHeaderMessage) + threadMessages
            } else {
                threadMessages
            }
        }

        // Prepare thread items from the complete threads
        return completeThreads.keys.map { uuid ->

            val owner = ownerStateFlow?.value
            val messagesForThread = completeThreads[uuid]

            val originalMessage = messagesForThread?.get(0)
            val chat = getChat()
            val isSenderOwner: Boolean = originalMessage?.sender == chat.contactIds.firstOrNull()

            createThreadItem(uuid?.value, owner, messagesForThread, originalMessage, chat, isSenderOwner)
        }
    }

    private fun createThreadItem(
        uuid: String?,
        owner: Contact?,
        messagesForThread: List<Message>?,
        originalMessage: Message?,
        chat: Chat?,
        isSenderOwner: Boolean
    ): ThreadItem {

        val senderInfo = if (isSenderOwner) {
            Pair(owner?.alias, owner?.getColorKey())
        } else {
            Pair(
                originalMessage?.senderAlias?.value?.toContactAlias(),
                originalMessage?.getColorKey()
            )
        }

        val senderPhotoUrl = if (isSenderOwner) owner?.photoUrl else originalMessage?.senderPic

        val repliesList = messagesForThread?.drop(1)?.distinctBy { it.senderAlias }

        val imageAttachment = originalMessage?.retrieveImageUrlAndMessageMedia()?.let { mediaData ->
            Pair(mediaData.first, mediaData.second?.localFile)
        }
        val videoAttachment: File? = originalMessage?.messageMedia?.let { nnMessageMedia ->
            if (nnMessageMedia.mediaType.isVideo) { nnMessageMedia.localFile } else null
        }
        val fileAttachment: FileAttachment? = originalMessage?.messageMedia?.let { nnMessageMedia ->
            if (nnMessageMedia.mediaType.isImage || nnMessageMedia.mediaType.isAudio) {
                null
            } else {
                nnMessageMedia.localFile?.let { nnFile ->
                    val pageCount = if (nnMessageMedia.mediaType.isPdf) {
                        val fileDescriptor =
                            ParcelFileDescriptor.open(nnFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(fileDescriptor)
                        renderer.pageCount
                    } else {
                        0
                    }

                    FileAttachment(
                        nnMessageMedia.fileName,
                        FileSize(nnFile.length()),
                        nnMessageMedia.mediaType.isPdf,
                        pageCount
                    )
                }
            }
        }

        val audioAttachment: Boolean? = originalMessage?.messageMedia?.let { nnMessageMedia ->
            if (nnMessageMedia.mediaType.isAudio) {
                true
            } else {
                null
            }
        }

        val threadMessage = originalMessage?.messageContentDecrypted?.value ?: ""

        return ThreadItem(
            aliasAndColorKey = senderInfo,
            photoUrl = senderPhotoUrl,
            date = originalMessage?.date?.chatTimeFormat() ?: "",
            message = threadMessage.replacingHighlightedDelimiters(),
            highlightedTexts = threadMessage.highlightedTexts(),
            usersReplies = createReplyUserHolders(repliesList, chat, owner),
            usersCount = repliesList?.size ?: 0,
            repliesAmount = String.format(app.getString(R.string.replies_amount), messagesForThread?.drop(1)?.size?.toString() ?: "0"),
            lastReplyDate = messagesForThread?.last()?.date?.timeAgo(),
            uuid = uuid ?: "",
            imageAttachment = imageAttachment,
            videoAttachment = videoAttachment,
            fileAttachment = fileAttachment,
            audioAttachment = audioAttachment
        )
    }

    private fun createReplyUserHolders(
        repliesList: List<Message>?,
        chat: Chat?,
        owner: Contact?
    ): List<ReplyUserHolder>? {
        return repliesList?.take(6)?.map {
            val isSenderOwner: Boolean = it.sender == chat?.contactIds?.firstOrNull()

            ReplyUserHolder(
                photoUrl = if (isSenderOwner) owner?.photoUrl else it.senderPic,
                alias = if (isSenderOwner) owner?.alias else it.senderAlias?.value?.toContactAlias(),
                colorKey = if (isSenderOwner) owner?.getColorKey() ?: "" else it.getColorKey()
            )
        }
    }

    fun navigateToThreadDetail(uuid: String) {
        viewModelScope.launch(mainImmediate) {
            navigator.toChatTribeThread(ChatId(args.argChatId), ThreadUUID(uuid))
        }
    }

}
