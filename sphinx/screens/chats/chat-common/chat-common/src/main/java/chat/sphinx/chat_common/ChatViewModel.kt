package chat.sphinx.chat_common

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(

): BaseViewModel<ChatViewState>(ChatViewState.Idle) {
}
