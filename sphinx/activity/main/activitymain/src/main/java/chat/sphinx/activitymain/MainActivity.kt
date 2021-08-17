package chat.sphinx.activitymain

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.activitymain.databinding.ActivityMainBinding
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.activitymain.ui.MainViewState
import chat.sphinx.activitymain.ui.MotionLayoutNavigationActivity
import chat.sphinx.insetter_activity.InsetPadding
import chat.sphinx.insetter_activity.InsetterActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import chat.sphinx.resources.R as R_common

@AndroidEntryPoint
internal class MainActivity: MotionLayoutNavigationActivity<
        MainViewState,
        MainViewModel,
        PrimaryNavigationDriver,
        MainViewModel,
        ActivityMainBinding,
        >(R.layout.activity_main), InsetterActivity
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

    companion object {
        // Setting these here at initial load time will negate the need to query the view
        // parameters again. This is ok as we're locked to portrait mode, and allows activity
        // re-creation after the first call is made to InsetterActivity.
        private var statusBarInsets: InsetPadding? = null
        private var navigationBarInsets: InsetPadding? = null
    }

    override val statusBarInsetHeight: InsetPadding by lazy(LazyThreadSafetyMode.NONE) {
        statusBarInsets ?: binding.layoutConstraintMainStatusBar.let {
            InsetPadding(
                it.paddingLeft,
                it.paddingRight,
                it.paddingTop,
                it.paddingBottom
            )
        }.also { statusBarInsets = it }
    }

    override val navigationBarInsetHeight: InsetPadding by lazy(LazyThreadSafetyMode.NONE) {
        navigationBarInsets ?: binding.layoutConstraintMainNavigationBar.let {
            InsetPadding(
                it.paddingLeft,
                it.paddingRight,
                it.paddingTop,
                it.paddingBottom
            )
        }.also { navigationBarInsets = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R_common.style.AppPostLaunchTheme)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setTransitionListener(binding.layoutMotionMain)

        intent.data?.let { intentData ->
            handleDeepLink(intentData)
        }

        binding.layoutConstraintMainStatusBar.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }
        binding.layoutConstraintMainNavigationBar.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        binding.viewMainInputLock.setOnClickListener { viewModel }
    }

    override fun onStart() {
        super.onStart()

        // Authentication
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel
                .authenticationDriver
                .navigationRequestSharedFlow
                .collect { request ->
                    viewModel
                        .authenticationDriver
                        .executeNavigationRequest(authenticationNavController, request)
                }
        }

        // Detail Screen
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel
                .detailDriver
                .navigationRequestSharedFlow
                .collect { request ->
                    if (
                        viewModel
                            .detailDriver
                            .executeNavigationRequest(detailNavController, request)
                    ) {
                        if (detailNavController.previousBackStackEntry == null) {
                            viewModel.updateViewState(MainViewState.DetailScreenInactive)
                        } else {
                            viewModel.updateViewState(MainViewState.DetailScreenActive)
                        }
                    }
                }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        intent.data?.let { intentData ->
            handleDeepLink(intentData)
        }
    }

    private fun handleDeepLink(data: Uri) {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.handleDeepLink(
                data.toString()
            )
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: MainViewState) {
        when (viewState) {
            is MainViewState.DetailScreenActive -> {
                binding.layoutMotionMain.setTransitionDuration(400)
            }
            is MainViewState.DetailScreenInactive -> {
                binding.layoutMotionMain.setTransitionDuration(250)
            }
        }
        viewState.transitionToEndSet(binding.layoutMotionMain)
    }

    override fun onCreatedRestoreMotionScene(viewState: MainViewState, binding: ActivityMainBinding) {
        viewState.restoreMotionScene(binding.layoutMotionMain)
    }

    override fun getMotionLayouts(): Array<MotionLayout> {
        return arrayOf(binding.layoutMotionMain)
    }

    private var transitionInProgress = false
    override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {
        transitionInProgress = true
    }

    // To Handle swipe behaviour
    override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
        transitionInProgress = false
            if (
                currentId == MainViewState.DetailScreenInactive.endSetId &&
                detailNavController.previousBackStackEntry != null
            ) {
                lifecycleScope.launch {
                    viewModel.detailDriver.submitNavigationRequest(
                        PopBackStack(R.id.navigation_detail_blank_fragment)
                    )
                }
            }
    }

    override fun onBackPressed() {
        when {

            // AuthenticationNavController
            authenticationNavController.previousBackStackEntry != null -> {
                // Authentication Screen has a callback to handle it automatically
                super.onBackPressed()
            }
            // DetailNavController
            detailNavController.previousBackStackEntry != null -> {
                // Downside to this is that DetailScreens cannot add
                // back press callbacks, but that's why they're detail screens
                if (!transitionInProgress) {
                    lifecycleScope.launch {
                        viewModel.detailDriver.submitNavigationRequest(PopBackStack())
                    }
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
