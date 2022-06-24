package chat.sphinx.chat_common.ui.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import android.view.OrientationEventListener
import android.widget.MediaController
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.databinding.ActivityFullscreenVideoBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.launch

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@AndroidEntryPoint
internal class
FullscreenVideoActivity : AppCompatActivity() {
    private lateinit var orientationListener: OrientationEventListener

    private val onStopSupervisor: OnStopSupervisor = OnStopSupervisor()
    private var currentViewState: FullscreenVideoViewState? = null

    private val viewModel: FullscreenVideoViewModel by viewModels()
    private val binding: ActivityFullscreenVideoBinding by viewBinding(ActivityFullscreenVideoBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_video)

        binding.apply {
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

            val controller = MediaController(root.context)
            controller.setAnchorView(videoViewContent)
            controller.setMediaPlayer(videoViewContent)
            videoViewContent.setMediaController(controller)

            viewModel.initializeVideo(videoViewContent)
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
            is FullscreenVideoViewState.MetaDataLoaded -> {
                optimizeVideoSize()
            }
            FullscreenVideoViewState.Idle -> { }
        }
    }
}