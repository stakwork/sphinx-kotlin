package chat.sphinx.chat_contact.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class ChatContactViewModel @Inject constructor(

): BaseViewModel<ChatContactViewState>(ChatContactViewState.Idle)
{
}
