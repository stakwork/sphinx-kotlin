package chat.sphinx.send_attachment.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_view_model_coordinator.ResponseHolder
import chat.sphinx.feature_view_model_coordinator.RequestCatcher
import chat.sphinx.kotlin_response.Response
import chat.sphinx.send_attachment.coordinator.SendAttachmentViewModelCoordinator
import chat.sphinx.send_attachment.navigation.BackType
import chat.sphinx.send_attachment.navigation.SendAttachmentNavigator
import chat.sphinx.send_attachment_view_model_coordinator.response.SendAttachmentResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SendAttachmentViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val sendAttachmentViewModelCoordinator: SendAttachmentViewModelCoordinator,
    val navigator: SendAttachmentNavigator
): BaseViewModel<SendAttachmentViewState>(dispatchers, SendAttachmentViewState.Idle)
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
                        response = Response.Success(
                            ResponseHolder(
                                requestHolder,
                                sendAttachmentResponse
                            )
                        ),
                        navigateBack = BackType.PopBackStack
                    )
                }
            }
        }
    }
}