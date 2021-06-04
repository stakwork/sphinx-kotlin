package chat.sphinx.authentication.navigation

import androidx.navigation.NavController
import chat.sphinx.hilt_qualifiers.AuthenticationDriver
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_authentication.state.AuthenticationStateManager
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.feature_authentication_view.navigation.AuthenticationNavigator
import javax.inject.Inject

@ActivityRetainedScoped
internal class SphinxAuthenticationNavigator @Inject constructor(
    val authenticationStateManager: AuthenticationStateManager,

    @AuthenticationDriver
    navigationDriver: BaseNavigationDriver<NavController>
): AuthenticationNavigator<NavController>(navigationDriver) {
    override suspend fun toAuthenticationView() {
        navigationDriver.submitNavigationRequest(ToAuthenticationView.get(authenticationStateManager))
    }

    override suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }
}
