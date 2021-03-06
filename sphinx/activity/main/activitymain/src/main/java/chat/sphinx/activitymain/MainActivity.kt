package chat.sphinx.activitymain

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.activitymain.databinding.ActivityMainBinding
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.resources.R as R_common
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_activity.NavigationActivity
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.NavigationRequest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity: NavigationActivity<
        MainViewModel,
        PrimaryNavigationDriver,
        MainViewModel,
        ActivityMainBinding,
        >(R.layout.activity_main)
{
    override val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    override val navController: NavController by lazy(LazyThreadSafetyMode.NONE) {
        binding.navHostFragmentPrimary.findNavController()
    }
    private val authenticationNavController: NavController by lazy(LazyThreadSafetyMode.NONE) {
        binding.navHostFragmentAuthentication.findNavController()
    }
    private val detailNavController: NavController by lazy(LazyThreadSafetyMode.NONE) {
        binding.navHostFragmentDetail.findNavController()
    }

    override val viewModel: MainViewModel by viewModels()
    override val navigationViewModel: MainViewModel
        get() = viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R_common.style.AppPostLaunchTheme)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launchWhenStarted {
            viewModel
                .authenticationDriver
                .navigationRequestSharedFlow
                .collect { request ->
                    viewModel
                        .authenticationDriver
                        .executeNavigationRequest(authenticationNavController, request)
                }
        }

        lifecycleScope.launchWhenStarted {
            viewModel
                .detailDriver
                .navigationRequestSharedFlow
                .collect { request ->
                    viewModel
                        .detailDriver
                        .executeNavigationRequest(detailNavController, request)
                }
        }
    }

    override suspend fun onPostNavigationRequestExecution(request: NavigationRequest<NavController>) {
        super.onPostNavigationRequestExecution(request)
    }

    override fun onBackPressed() {
        when {

            // AuthenticationNavController
            authenticationNavController.previousBackStackEntry != null -> {
                // AuthenticationView has a callback to handle it automatically
                super.onBackPressed()
            }

            // DetailNavController
            detailNavController.previousBackStackEntry != null -> {
                // Downside to this is that DetailScreens cannot add
                // a backpress callbacks, but that's why they're detail screens
                lifecycleScope.launch {
                    viewModel.detailDriver.submitNavigationRequest(PopBackStack())
                }
            }

            // PrimaryNavController
            else -> {
                when {
                    onBackPressedDispatcher.hasEnabledCallbacks() -> {
                        super.onBackPressed()
                    }
                    navController.previousBackStackEntry == null -> {
                        super.onBackPressed()
                    }
                    else -> {
                        lifecycleScope.launch {
                            viewModel.navigationDriver.submitNavigationRequest(PopBackStack())
                        }
                    }
                }
            }
        }
    }
}
