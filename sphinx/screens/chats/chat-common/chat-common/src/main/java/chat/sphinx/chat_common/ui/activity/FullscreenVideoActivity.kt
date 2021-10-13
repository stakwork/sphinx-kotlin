package chat.sphinx.chat_common.ui.activity

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.View.OnTouchListener
import android.widget.RelativeLayout
import android.view.OrientationEventListener
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.databinding.ActivityFullscreenVideoBinding
import chat.sphinx.chat_common.ui.viewstate.messageholder.toTimestamp
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.launch

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@AndroidEntryPoint
internal class FullscreenVideoActivity : AppCompatActivity() {
    private lateinit var orientationListener: OrientationEventListener

    private val onStopSupervisor: OnStopSupervisor = OnStopSupervisor()
    private var currentViewState: FullscreenVideoViewState? = null

    private val viewModel: FullscreenVideoViewModel by viewModels()
    private val binding: ActivityFullscreenVideoBinding by viewBinding(ActivityFullscreenVideoBinding::bind)

    private val mHideHandler = Handler()

    private val mShowPart2Runnable = Runnable { // Delayed display of UI elements
        lifecycleScope.launch(viewModel.mainImmediate) {
            binding.layoutConstraintVideoControls.visible
        }
    }

    private val mHideRunnable = Runnable { hide() }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_video)

        binding.apply {
            // Set up the user interaction to manually show or hide the system UI.
            videoViewContent.setOnClickListener {
                toggle()
            }
            root.setOnClickListener {
                toggle()
            }
            viewModel.videoPlayerController.setVideo(binding.videoViewContent)

            textViewPlayPauseButton.setOnClickListener {
                viewModel.videoPlayerController.togglePlayPause()
            }
            // Upon interacting with UI controls, delay any scheduled hide()
            // operations to prevent the jarring behavior of controls going away
            // while interacting with the UI.
            layoutConstraintVideoControls.setOnTouchListener(mDelayHideTouchListener)
//            textViewCurrentTime.setOnTouchListener(mDelayHideTouchListener)
//            seekBarCurrentProgress.setOnTouchListener(mDelayHideTouchListener)

            orientationListener =  object : OrientationEventListener(this@FullscreenVideoActivity) {
                override fun onOrientationChanged(orientation: Int) {
                    val rotation = when {
                        orientation <= 45 -> Surface.ROTATION_0
                        orientation <= 135 -> Surface.ROTATION_90
                        orientation <= 225 -> Surface.ROTATION_180
                        orientation <= 315 -> Surface.ROTATION_270
                        else -> Surface.ROTATION_0
                    }
                    when(rotation) {
                        Surface.ROTATION_0 -> {
                            setOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                        }
                        Surface.ROTATION_90 -> {
                            setOrientation( ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                        }
                        Surface.ROTATION_180 -> {
                            setOrientation( ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
                        }
                        Surface.ROTATION_270 -> {
                            setOrientation( ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        }
                    }
                }
            }
            viewModel.initializeVideo()
        }
    }

    @Synchronized
    fun setOrientation(orientation: Int) {
        if (requestedOrientation != orientation) {
            // TODO: Delayed orientation update
            requestedOrientation = orientation
            optimizeVideoSize()
        }
    }

    fun optimizeVideoSize() {
        binding.videoViewContent.apply {
            // Video Width > Video Height
            layoutParams = if (viewModel.currentViewState.videoDimensions.first > viewModel.currentViewState.videoDimensions.second) {
                RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
            } else {
                RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                )
            }

        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (binding.layoutConstraintVideoControls.isVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            // Hide UI first
            binding.layoutConstraintVideoControls.gone

            // Schedule a runnable to remove the status and navigation bar after a delay
            mHideHandler.removeCallbacks(mShowPart2Runnable)
        }
    }

    private fun show() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            // Show the system bar
            if (Build.VERSION.SDK_INT >= 30) {
                binding.videoViewContent.windowInsetsController!!.show(
                    WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
                )
            } else {
                binding.videoViewContent.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            }
            // Schedule a runnable to display UI elements after a delay
            mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
        }
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [.AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [.AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }

    override fun onStart() {
        super.onStart()
        subscribeToViewStateFlow()
        orientationListener.enable()
    }

    override fun onPause() {
        super.onPause()
        viewModel.videoPlayerController.pause()
    }

    override fun onStop() {
        super.onStop()
        viewModel.videoPlayerController.pause()
        orientationListener.disable()
    }

    private fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->
                if (currentViewState != viewState) {
                    currentViewState = viewState
                    onViewStateFlowCollect(viewState)
                }
            }
        }
    }

    private fun onViewStateFlowCollect(viewState: FullscreenVideoViewState) {
        when (viewState) {
            is FullscreenVideoViewState.VideoMessage -> {
                binding.textViewVideoMessageText.text = viewState.name
                binding.layoutConstraintTitle.visible
            }
            is FullscreenVideoViewState.MetaDataLoaded -> {
                binding.seekBarCurrentProgress.max = viewState.duration
                binding.textViewCurrentTime.text = viewState.duration.toLong().toTimestamp()

                optimizeVideoSize()
            }
            is FullscreenVideoViewState.CurrentTimeUpdate -> {
                binding.seekBarCurrentProgress.progress = viewState.currentTime
                binding.textViewCurrentTime.text = viewState.currentTime.toLong().toTimestamp()
            }
            is FullscreenVideoViewState.ContinuePlayback -> {
                binding.textViewPlayPauseButton.text = binding.root.context.getString(R.string.material_icon_name_pause_button)
            }
            is FullscreenVideoViewState.PausePlayback -> {
                binding.textViewPlayPauseButton.text = binding.root.context.getString(R.string.material_icon_name_play_button)
            }
            is FullscreenVideoViewState.CompletePlayback -> {
                binding.seekBarCurrentProgress.progress = 0
                binding.textViewCurrentTime.text = 0L.toTimestamp()
                binding.textViewPlayPauseButton.text = binding.root.context.getString(R.string.material_icon_name_play_button)
            }
            FullscreenVideoViewState.Idle -> {

            }
        }
    }
}