package chat.sphinx.video_screen.ui.watch

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.resources.toTimestamp
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.adapter.VideoFeedItemsAdapter
import chat.sphinx.video_screen.adapter.VideoFeedItemsFooterAdapter
import chat.sphinx.video_screen.databinding.FragmentVideoWatchScreenBinding
import chat.sphinx.video_screen.ui.viewstate.PlayingVideoViewState
import chat.sphinx.video_screen.ui.viewstate.SelectedVideoViewState
import chat.sphinx.video_screen.ui.viewstate.VideoFeedScreenViewState
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.isYoutubeVideo
import chat.sphinx.wrapper_common.feed.youtubeVideoId
import chat.sphinx.wrapper_common.hhmmElseDate
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class VideoFeedWatchScreenFragment: BaseFragment<
        VideoFeedScreenViewState,
        VideoFeedWatchScreenViewModel,
        FragmentVideoWatchScreenBinding
        >(R.layout.fragment_video_watch_screen)
{
    private lateinit var orientationListener: OrientationEventListener

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .build()
    }

    override val binding: FragmentVideoWatchScreenBinding by viewBinding(FragmentVideoWatchScreenBinding::bind)
    override val viewModel: VideoFeedWatchScreenViewModel by viewModels()

    private var dragging: Boolean = false

    private val mHideHandler = Handler(Looper.getMainLooper())
    private val mHideRunnable = Runnable { toggleRemoteVideoControllers() }

    companion object {
        private const val AUTO_HIDE_DELAY_MILLIS = 3000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupOrientationListener()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupItems()
        setupVideoPlayer()
    }

    private fun setupItems() {
        binding.includeLayoutVideoItemsList?.let {
            it.recyclerViewVideoList.apply {
                val linearLayoutManager = LinearLayoutManager(context)
                val videoFeedItemsAdapter = VideoFeedItemsAdapter(
                    imageLoader,
                    viewLifecycleOwner,
                    onStopSupervisor,
                    viewModel,
                    viewModel
                )
                val videoListFooterAdapter = VideoFeedItemsFooterAdapter(requireActivity() as InsetterActivity)
                this.setHasFixedSize(false)
                layoutManager = linearLayoutManager
                adapter = ConcatAdapter(videoFeedItemsAdapter, videoListFooterAdapter)
                itemAnimator = null
            }
        }
    }

    private fun setupVideoPlayer() {
        binding.includeLayoutVideoPlayer.apply {
            viewModel.setVideoView(videoViewVideoPlayer)

            videoViewVideoPlayer.setOnClickListener {
                toggleRemoteVideoControllers()
            }

            textViewPlayPauseButton.setOnClickListener {
                viewModel.togglePlayPause()
            }

            seekBarCurrentProgress.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            textViewCurrentTime.text = progress.toLong().toTimestamp()
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                        dragging = true
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        viewModel.seekTo(seekBar.progress)
                        dragging = false
                    }
                }
            )
        }
    }

    private fun setupOrientationListener() {
        orientationListener =  object : OrientationEventListener(requireContext()) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation = when {
                    orientation <= 45 -> Surface.ROTATION_0
                    orientation <= 135 -> Surface.ROTATION_90
                    orientation <= 225 -> Surface.ROTATION_180
                    orientation <= 315 -> Surface.ROTATION_270
                    else -> Surface.ROTATION_0
                }
                when(rotation) {
                    Surface.ROTATION_90,
                    Surface.ROTATION_180 -> {
                        viewModel.goToFullscreenVideo()
                    }
                    Surface.ROTATION_0,
                    Surface.ROTATION_270 -> {
                        // Do nothing...
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        orientationListener.enable()
    }

    override fun onStop() {
        super.onStop()
        orientationListener.disable()
    }

    override suspend fun onViewStateFlowCollect(viewState: VideoFeedScreenViewState) {
        @Exhaustive
        when (viewState) {
            is VideoFeedScreenViewState.Idle -> {}

            is VideoFeedScreenViewState.FeedLoaded -> {
                binding.apply {
                    includeLayoutVideoItemsList?.textViewVideosListCount?.text = viewState.items.count().toString()

                    includeLayoutVideoPlayer.apply {
                        textViewContributorName.text = viewState.title.value

                        viewState.imageToShow?.let {
                            imageLoader.load(
                                imageViewContributorImage,
                                it.value,
                                imageLoaderOptions
                            )
                        }
                    }
                }
            }
        }
    }

    private fun toggleRemoteVideoControllers() {
        if (binding.includeLayoutVideoPlayer.layoutConstraintBottomControls.isVisible) {
            binding.includeLayoutVideoPlayer.layoutConstraintBottomControls.gone
        } else {
            binding.includeLayoutVideoPlayer.layoutConstraintBottomControls.visible
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
    }

    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.selectedVideoStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is SelectedVideoViewState.Idle -> {}

                    is SelectedVideoViewState.VideoSelected -> {
                        binding.includeLayoutVideoPlayer.apply {

                            textViewVideoTitle?.text = viewState.title.value
                            textViewVideoDescription?.text = viewState.description?.value ?: ""
                            textViewVideoPublishedDate?.text = viewState.date?.hhmmElseDate()

                            if (viewState.url.isYoutubeVideo()) {

                                layoutConstraintVideoViewContainer.gone
                                webViewYoutubeVideoPlayer.visible

                                webViewYoutubeVideoPlayer.settings.apply {
                                    javaScriptEnabled = true
                                }

                                webViewYoutubeVideoPlayer.loadData(
                                    "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube-nocookie.com/embed/%s\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>",
                                    FeedUrl.YOUTUBE_WEB_VIEW_MIME_TYPE,
                                    FeedUrl.YOUTUBE_WEB_VIEW_ENCODING
                                )
                            } else {

                                layoutConstraintVideoViewContainer.visible
                                webViewYoutubeVideoPlayer.gone

                                viewModel.initializeVideo(
                                    viewState.url.value.toUri(),
                                    viewState.duration?.value?.toInt()
                                )
                            }
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.playingVideoStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is PlayingVideoViewState.Idle -> {}

                    is PlayingVideoViewState.MetaDataLoaded -> {
                        binding.includeLayoutVideoPlayer.seekBarCurrentProgress.max = viewState.duration
                        binding.includeLayoutVideoPlayer.textViewCurrentTime.text = viewState.duration.toLong().toTimestamp()
                        //                optimizeVideoSize()
                    }
                    is PlayingVideoViewState.CurrentTimeUpdate -> {
                        if (!dragging) {
                            binding.includeLayoutVideoPlayer.seekBarCurrentProgress.progress = viewState.currentTime
                            binding.includeLayoutVideoPlayer.textViewCurrentTime.text = (viewState.duration - viewState.currentTime).toLong().toTimestamp()
                        }
                    }
                    is PlayingVideoViewState.ContinuePlayback -> {
                        binding.includeLayoutVideoPlayer.textViewPlayPauseButton.text = binding.root.context.getString(R.string.material_icon_name_pause_button)
                    }
                    is PlayingVideoViewState.PausePlayback -> {
                        binding.includeLayoutVideoPlayer.textViewPlayPauseButton.text = binding.root.context.getString(R.string.material_icon_name_play_button)
                    }
                    is PlayingVideoViewState.CompletePlayback -> {
                        binding.includeLayoutVideoPlayer.seekBarCurrentProgress.progress = 0
                        binding.includeLayoutVideoPlayer.textViewCurrentTime.text = 0L.toTimestamp()
                        binding.includeLayoutVideoPlayer.textViewPlayPauseButton.text = binding.root.context.getString(R.string.material_icon_name_play_button)
                    }
                }
            }
        }
    }
}
