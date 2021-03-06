package chat.sphinx.add_friend.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class AddFriendViewModel @Inject constructor(

): BaseViewModel<AddFriendViewState>(AddFriendViewState.Idle)
{
}
