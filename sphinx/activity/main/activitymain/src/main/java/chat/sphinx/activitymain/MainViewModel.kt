package chat.sphinx.activitymain

import androidx.lifecycle.viewModelScope
import chat.sphinx.activitymain.navigation.drivers.AuthenticationNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.activitymain.ui.MainViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_activity.NavigationViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.state.AuthenticationState
import io.matthewnelson.concept_authentication.state.AuthenticationStateManager
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class MainViewModel @Inject constructor(
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val authenticationStateManager: AuthenticationStateManager,
    val authenticationDriver: AuthenticationNavigationDriver,
    val detailDriver: DetailNavigationDriver,
    private val dispatchers: CoroutineDispatchers,
    override val navigationDriver: PrimaryNavigationDriver
): BaseViewModel<MainViewState>(MainViewState.Idle), NavigationViewModel<PrimaryNavigationDriver>
{
    init {
        viewModelScope.launch(dispatchers.mainImmediate) {
            authenticationStateManager.authenticationStateFlow.collect { state ->
                if (state is AuthenticationState.Required.LoggedOut) {
                    authenticationCoordinator.submitAuthenticationRequest(
                        AuthenticationRequest.LogIn(privateKey = null)
                    )
                }
            }
        }
    }
}
