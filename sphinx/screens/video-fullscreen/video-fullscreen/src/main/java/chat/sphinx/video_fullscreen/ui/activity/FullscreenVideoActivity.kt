package chat.sphinx.video_fullscreen.ui.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.RelativeLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.resources.toTimestamp
import chat.sphinx.video_fullscreen.R
import chat.sphinx.video_fullscreen.databinding.ActivityFullscreenVideoBinding
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.youtubeVideoId
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

        delayedHide(1000)
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
            is FullscreenVideoViewState.YoutubeVideo -> {
                binding.apply {
                    videoViewContent.gone
                    layoutConstraintBottomControls.gone

                    webViewYoutubeVideoPlayer.visible

                    webViewYoutubeVideoPlayer.settings.apply {
                        javaScriptEnabled = true
                    }

                    viewState.youtubeFeedId?.let { youtubeFeedId ->
                        webViewYoutubeVideoPlayer.loadData(
                            String.format(FeedUrl.YOUTUBE_WEB_VIEW_IFRAME_PATTERN, youtubeFeedId.youtubeVideoId()),
                            FeedUrl.YOUTUBE_WEB_VIEW_MIME_TYPE,
                            FeedUrl.YOUTUBE_WEB_VIEW_ENCODING
                        )
                    }
                }
            }
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