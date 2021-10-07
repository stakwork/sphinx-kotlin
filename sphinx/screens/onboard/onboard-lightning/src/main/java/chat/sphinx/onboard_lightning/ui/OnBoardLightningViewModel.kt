package chat.sphinx.onboard_lightning.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.onboard_common.OnBoardStepHandler
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.onboard_lightning.navigation.OnBoardLightningNavigator
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class OnBoardLightningViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: OnBoardLightningNavigator,
    private val onBoardStepHandler: OnBoardStepHandler,
): BaseViewModel<
        OnBoardLightningViewState
        >(dispatchers, OnBoardLightningViewState.Idle)
{

    private var nextScreenJob: Job? = null
    fun nextScreen(inviterData: OnBoardInviterData) {
        if (nextScreenJob?.isActive == true) {
            return
        }

        nextScreenJob = viewModelScope.launch {
            val step2 = onBoardStepHandler.persistOnBoardStep2Data(inviterData)

            if (step2 != null) {
                navigator.toOnBoardNameScreen(step2)
            } else {
                // TODO: Handle Persistence Error
            }
        }
    }

}