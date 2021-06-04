package chat.sphinx.add_friend.ui

import chat.sphinx.add_friend.navigation.AddFriendNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class AddFriendViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: AddFriendNavigator
): BaseViewModel<AddFriendViewState>(dispatchers, AddFriendViewState.Idle)
{
}
