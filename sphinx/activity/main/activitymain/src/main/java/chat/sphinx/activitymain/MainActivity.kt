package chat.sphinx.activitymain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
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
        private var statusBarInsets: InsetPadding? = null
        private var navigationBarInsets: InsetPadding? = null
        private var keyboardInsets: InsetPadding? = null
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null){
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    override val statusBarInsetHeight: InsetPadding by lazy(LazyThreadSafetyMode.NONE) {
        statusBarInsets ?: binding.layoutConstraintMainStatusBar.let {
            InsetPadding(
                it.paddingLeft,
                it.paddingTop,
                it.paddingRight,
                it.paddingBottom
            )
        }.also { statusBarInsets = it }
    }

    override val navigationBarInsetHeight: InsetPadding by lazy(LazyThreadSafetyMode.NONE) {
        navigationBarInsets ?: binding.layoutConstraintMainNavigationBar.let {
            InsetPadding(
                it.paddingLeft,
                it.paddingTop,
                it.paddingRight,
                it.paddingBottom
            )
        }.also { navigationBarInsets = it }
    }

    override val keyboardInsetHeight: InsetPadding
        get() = keyboardInsets ?: navigationBarInsetHeight


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R_common.style.AppPostLaunchTheme)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setTransitionListener(binding.layoutMotionMain)

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

        addWindowInsetChangeListener()
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

    override fun onPause() {
        super.onPause()

        viewModel.syncActions()
    }

    override var isKeyboardVisible: Boolean = false
    private fun addWindowInsetChangeListener() {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, windowInsets ->

            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            isKeyboardVisible = imeInsets.bottom > 0

            keyboardInsets = if (isKeyboardVisible) {
                InsetPadding(imeInsets.left, imeInsets.top, imeInsets.right, imeInsets.bottom)
            } else {
                null
            }

            windowInsets
        }
    }
}

