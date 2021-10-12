package chat.sphinx.chat_common.ui.activity

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_common.util.VideoPlayerController
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.retrieveTextToShow
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
internal class FullscreenVideoViewModel @Inject constructor(
    val app: Application,
    handle: SavedStateHandle,
    messageRepository: MessageRepository,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FullscreenVideoSideEffect,
        FullscreenVideoViewState
        >(dispatchers, FullscreenVideoViewState.Idle) {

    private val args: FullscreenVideoActivityArgs by handle.navArgs()
    private val messageId = MessageId(args.argMessageId)

    internal val videoPlayerController: VideoPlayerController by lazy {
        VideoPlayerController(
            app
        )
    }

    private val messageSharedFlow: SharedFlow<Message?> = flow {
        emitAll(messageRepository.getMessageById(messageId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    private suspend fun getMessage(): Message? {
        messageSharedFlow.replayCache.firstOrNull()?.let { message ->
            return message
        }

        messageSharedFlow.firstOrNull()?.let { message ->
            return message
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

        return message
    }

    private suspend fun getVideoFile(): File? {
        return getMessage()?.messageMedia?.localFile
    }

    private suspend fun getVideoTitle(): String? {
        return getMessage()?.retrieveTextToShow()
    }

    fun initializeVideo() {
        viewModelScope.launch(mainImmediate) {
            getVideoTitle()?.let { title ->
                updateViewState(FullscreenVideoViewState.VideoMessage(title))
            }
            getVideoFile()?.let { videoFile ->
                videoPlayerController.initializeVideo(videoFile)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        videoPlayerController.clear()
    }
}