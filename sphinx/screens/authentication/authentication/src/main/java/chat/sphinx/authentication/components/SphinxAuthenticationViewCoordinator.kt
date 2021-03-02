package chat.sphinx.authentication.components

import androidx.navigation.NavController
import chat.sphinx.authentication.navigation.SphinxAuthenticationNavigator
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import io.matthewnelson.feature_authentication_view.navigation.AuthenticationViewCoordinator
import javax.inject.Inject

@ActivityRetainedScoped
internal class SphinxAuthenticationViewCoordinator @Inject constructor(
    authenticationNavigator: SphinxAuthenticationNavigator,
    authenticationManager: AuthenticationCoreManager
): AuthenticationViewCoordinator<NavController>(
    authenticationNavigator,
    authenticationManager
)
