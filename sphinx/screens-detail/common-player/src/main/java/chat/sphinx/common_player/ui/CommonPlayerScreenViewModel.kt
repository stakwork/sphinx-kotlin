package chat.sphinx.common_player.ui

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import chat.sphinx.common_player.navigation.CommonPlayerNavigator
import chat.sphinx.common_player.viewstate.CommonPlayerScreenViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_feed.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class CommonPlayerScreenViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: CommonPlayerNavigator
): SideEffectViewModel<
        FragmentActivity,
        CommonPlayerScreenSideEffect,
        CommonPlayerScreenViewState,
        >(dispatchers, CommonPlayerScreenViewState.Idle)
{
    fun recommendedItemSelected(item: FeedItem) {

    }
}