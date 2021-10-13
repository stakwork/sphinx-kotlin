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
    private val mHideRunnable = Runnable { toggle() }

    companion object {
        /**
         * If [.AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_video)

        binding.apply {
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

            seekBarCurrentProgress.setOnTouchListener { _, _ -> true }

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

    private fun optimizeVideoSize() {
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

        delayedHide(500)
    }

    private fun toggle() {
        if (binding.layoutConstraintVideoControls.isVisible) {
            binding.layoutConstraintVideoControls.gone
        } else {
            binding.layoutConstraintVideoControls.visible
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
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
            FullscreenVideoViewState.Idle -> { }
        }
    }
}