package chat.sphinx.chat_tribe.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class ChatTribeViewModel @Inject constructor(

): BaseViewModel<ChatTribeViewState>(ChatTribeViewState.Idle)
{
}
