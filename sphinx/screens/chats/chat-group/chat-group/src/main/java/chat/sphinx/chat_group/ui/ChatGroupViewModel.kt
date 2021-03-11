package chat.sphinx.chat_group.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class ChatGroupViewModel @Inject constructor(

): BaseViewModel<ChatGroupViewState>(ChatGroupViewState.Idle)
{
}
