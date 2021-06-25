package chat.sphinx.send_attachment.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_view_model_coordinator.ResponseHolder
import chat.sphinx.feature_view_model_coordinator.RequestCatcher
import chat.sphinx.kotlin_response.Response
import chat.sphinx.send_attachment.coordinator.SendAttachmentViewModelCoordinator
import chat.sphinx.send_attachment_view_model_coordinator.response.SendAttachmentResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.sideeffect.SideEffect
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SendAttachmentViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val sendAttachmentViewModelCoordinator: SendAttachmentViewModelCoordinator,
    private val handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        SideEffect<Context>,
        SendAttachmentViewState
        >(
    dispatchers,
    handle.navArgs<SendAttachmentFragmentArgs>().let {
        SendAttachmentViewState.LayoutVisibility(
            it.value.argIsConversation,
        )
    },
)
{

    private val requestCatcher = RequestCatcher(
        viewModelScope,
        sendAttachmentViewModelCoordinator,
        mainImmediate
    )

    fun processResponse(sendAttachmentResponse: SendAttachmentResponse) {
        viewModelScope.launch(mainImmediate) {
            requestCatcher.getCaughtRequestStateFlow().collect { list ->
                list.firstOrNull()?.let { requestHolder ->
                    sendAttachmentViewModelCoordinator.submitResponse(
                        Response.Success(
                            ResponseHolder(
                                requestHolder,
                                sendAttachmentResponse
                            )
                        ),
                        Any()
                    )
                }
            }
        }
    }
}
