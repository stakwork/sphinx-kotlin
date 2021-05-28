package chat.sphinx.join_tribe.ui

import androidx.lifecycle.SavedStateHandle
import chat.sphinx.join_tribe.navigation.JoinTribeNavigator
import chat.sphinx.wrapper_common.TribeJoinLink
import chat.sphinx.wrapper_common.isValidTribeJoinLink
import chat.sphinx.wrapper_common.toTribeJoinLink
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlin.concurrent.schedule
import java.util.*
import javax.inject.Inject

@HiltViewModel
internal class JoinTribeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    val navigator: JoinTribeNavigator
): BaseViewModel<JoinTribeViewState>(dispatchers, JoinTribeViewState.LoadingTribeInfo)
{
    private val args: JoinTribeFragmentArgs by savedStateHandle.navArgs()

    fun loadTribeData() {
        args.argTribeLink.toTribeJoinLink()?.let { tribeJoinLink ->
            viewStateContainer.updateViewState(JoinTribeViewState.TribeInfo(tribeJoinLink.tribeHost, tribeJoinLink.tribeUUID))
        } ?: run {
            viewStateContainer.updateViewState(JoinTribeViewState.LoadingTribeFailed)
        }
    }
}
