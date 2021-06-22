package chat.sphinx.send_attachment.ui

import chat.sphinx.send_attachment.navigation.SendAttachmentNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class SendAttachmentViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: SendAttachmentNavigator
): BaseViewModel<SendAttachmentViewState>(dispatchers, SendAttachmentViewState.Idle)
{
}