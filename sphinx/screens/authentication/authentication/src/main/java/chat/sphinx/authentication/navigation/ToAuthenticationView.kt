package chat.sphinx.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.navOptions
import app.cash.exhaustive.Exhaustive
import chat.sphinx.authentication.R
import io.matthewnelson.concept_authentication.state.AuthenticationState
import io.matthewnelson.android_feature_navigation.R as R_navigation
import io.matthewnelson.concept_authentication.state.AuthenticationStateManager
import io.matthewnelson.concept_navigation.NavigationRequest

internal class ToAuthenticationView private constructor(
    private val authenticationStateManager: AuthenticationStateManager
): NavigationRequest<NavController>() {

    companion object {
        @JvmSynthetic
        fun get(authenticationStateManager: AuthenticationStateManager): ToAuthenticationView =
            ToAuthenticationView(authenticationStateManager)
    }

    override fun navigate(controller: NavController) {
        try {
            // If exception is thrown it's not on the backstack and will
            // navigate, otherwise it will not navigate and the authentication
            // coordinator will continue with submitting the authentication
            // request to the currently running instance
            controller.getBackStackEntry(R.id.navigation_authentication_fragment)
            return
        } catch (e: IllegalArgumentException) {}

        controller.navigate(
            R.id.authentication_nav_graph,
            null,
            navOptions {
                anim {
                    @Exhaustive
                    when (authenticationStateManager.authenticationStateFlow.value) {
                        is AuthenticationState.NotRequired -> {
                            enter = R_navigation.anim.slide_in_bottom
                            popExit = R_navigation.anim.slide_out_bottom
                        }
                        is AuthenticationState.Required.InitialLogIn -> {
                            enter = R_navigation.anim.slide_in_bottom
                            popExit = R_navigation.anim.slide_out_left
                        }
                        is AuthenticationState.Required.LoggedOut -> {
                            // Enter immediately
                            popExit = R_navigation.anim.slide_out_bottom
                        }
                    }
                }
            }
        )
    }
}
