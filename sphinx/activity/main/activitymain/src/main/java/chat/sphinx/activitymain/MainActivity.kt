package chat.sphinx.activitymain

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
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
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.launch
import chat.sphinx.resources.R as R_common


@AndroidEntryPoint
class MainActivity: MotionLayoutNavigationActivity<
        MainViewState,
        MainViewModel,
        PrimaryNavigationDriver,
        MainViewModel,
        ActivityMainBinding,
        >(R.layout.activity_main), InsetterActivity
{
    override val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    private var sessionDepth = 0

    override val navController: NavController by lazy(LazyThreadSafetyMode.NONE) {
        binding.navHostFragmentPrimary.findNavController()
    }
    private val authenticationNavController: NavController by lazy(LazyThreadSafetyMode.NONE) {
        binding.navHostFragmentAuthentication.findNavController()
    }
    private val detailNavController: NavController by lazy(LazyThreadSafetyMode.NONE) {
        binding.navHostFragmentDetail.findNavController()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    override val viewModel: MainViewModel by viewModels()
    override val navigationViewModel: MainViewModel
        get() = viewModel

    companion object {
        private var statusBarInsets: InsetPadding? = null
        private var navigationBarInsets: InsetPadding? = null
        private var keyboardInsets: InsetPadding? = null

        var isActive = false
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
        askNotificationPermission()
        addWindowInsetChangeListener()

        intent.extras?.getString("chat_id")?.toLongOrNull()?.let { chatId ->
            handlePushNotification(chatId)
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

        sessionDepth++;
        if (sessionDepth == 1){
            viewModel.restoreContentFeedStatuses()
        }
    }

    override fun onResume() {
        super.onResume()

        isActive = true
    }

    override fun onStop() {
        super.onStop()

        if (sessionDepth > 0)
            sessionDepth--;
        if (sessionDepth == 0) {
            viewModel.saveContentFeedStatuses()
        }

        isActive = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        intent.dataString?.let { deepLink ->
            handleDeepLink(deepLink)
        }

        setIntent(intent)
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleDeepLink(deepLink: String) {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.handleDeepLink(deepLink)
        }
    }

    private fun handlePushNotification(chatId: Long) {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.handlePushNotification(chatId)
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
                    if (onBackPressedDispatcher.hasEnabledCallbacks()) {
                        super.onBackPressed()
                    } else {
                        lifecycleScope.launch {
                            viewModel.detailDriver.submitNavigationRequest(PopBackStack())
                        }
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
        viewModel.getDeleteExcessFileIfApplicable()

        isActive = false
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

