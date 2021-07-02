package chat.sphinx.invite_friend.ui

import android.content.Context
import chat.sphinx.invite_friend.navigation.InviteFriendNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class InviteFriendViewModel @Inject constructor(
    val navigator: InviteFriendNavigator,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        InviteFriendSideEffect,
        InviteFriendViewState
        >(dispatchers, InviteFriendViewState.Idle)
{

}
