package chat.sphinx.chat_common.ui.activity

import android.app.Application
import android.widget.VideoView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_common.util.VideoPlayerController
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_message.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class FullscreenVideoViewModel @Inject constructor(
    val app: Application,
    handle: SavedStateHandle,
    messageRepository: MessageRepository,
    dispatchers: CoroutineDispatchers,
): ViewModel(), CoroutineDispatchers by dispatchers  {

    private val args: FullscreenVideoActivityArgs by handle.navArgs()
    private val messageId = MessageId(args.argMessageId)

    private var videoPlayerController: VideoPlayerController? = null

    fun init(videoView: VideoView) {
        videoPlayerController = VideoPlayerController(
            app,
        )
    }
    private val messageSharedFlow: SharedFlow<Message?> = flow {
        emitAll(messageRepository.getMessageById(messageId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    suspend fun getVideoFile(): File? {
        messageSharedFlow.replayCache.firstOrNull()?.let { message ->
            return message.messageMedia?.localFile
        }

        messageSharedFlow.firstOrNull()?.let { message ->
            return message.messageMedia?.localFile
        }

        var message: Message? = null

        try {
            messageSharedFlow.collect {
                if (it != null) {
                    message = it
                    throw Exception()
                }
            }
        } catch (e: Exception) {}
        delay(25L)

        return message?.messageMedia?.localFile
    }

    override fun onCleared() {
        super.onCleared()

        videoPlayerController?.clear()
    }
}