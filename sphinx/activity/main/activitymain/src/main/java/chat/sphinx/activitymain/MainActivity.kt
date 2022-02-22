package chat.sphinx.activitymain

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.activity.viewModels
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.resources.R as R_common
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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

    override val navigationBarInsetHeight: InsetPadding
        get() {
            binding.layoutConstraintMainNavigationBar.let {
                return InsetPadding(
                    it.paddingLeft,
                    it.paddingRight,
                    it.paddingTop,
                    it.paddingBottom
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R_common.style.AppPostLaunchTheme)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setTransitionListener(binding.layoutMotionMain)

        binding.viewMainInputLock.setOnClickListener { viewModel }

        setWindowTransparency { statusBarSize, navigationBarSize ->
            binding.layoutConstraintMainStatusBar.setPadding(0,statusBarSize, 0,0)
            binding.layoutConstraintMainNavigationBar.setPadding(0,0, 0,navigationBarSize)
        }
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

        intent.dataString?.let { deepLink ->
            handleDeepLink(deepLink)
        }
    }

    private fun handleDeepLink(deepLink: String) {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.handleDeepLink(deepLink)
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

    private fun removeSystemInsets(view: View, listener: OnSystemInsetsChangedListener) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->

            val desiredBottomInset = calculateDesiredBottomInset(
                insets.systemWindowInsetTop,
                insets.systemWindowInsetBottom,
                listener
            )

            ViewCompat.onApplyWindowInsets(
                view,
                insets.replaceSystemWindowInsets(0, 0, 0, desiredBottomInset)
            )
        }
    }

    private fun calculateDesiredBottomInset(
        topInset: Int,
        bottomInset: Int,
        listener: OnSystemInsetsChangedListener
    ): Int {
        val hasKeyboard = isKeyboardAppeared(bottomInset)
        val desiredBottomInset = if (hasKeyboard) bottomInset else 0
        listener(topInset, if (hasKeyboard) 0 else bottomInset)
        return desiredBottomInset
    }

    private fun setWindowTransparency(
        listener: OnSystemInsetsChangedListener = { _, _ -> }
    ) {
        removeSystemInsets(window.decorView, listener)
        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT
    }

    override var keyboardVisible: Boolean = false

    private fun isKeyboardAppeared(bottomInset: Int): Boolean {
        keyboardVisible = bottomInset / window.decorView.rootView.measuredHeight.toDouble() > .25
        return keyboardVisible
    }


}

typealias OnSystemInsetsChangedListener =
            (statusBarSize: Int, navigationBarSize: Int) -> Unit

