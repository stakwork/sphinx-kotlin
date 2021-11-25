package chat.sphinx.video_screen.ui.watch

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.resources.toTimestamp
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.adapter.VideoFeedItemsAdapter
import chat.sphinx.video_screen.databinding.FragmentVideoWatchScreenBinding
import chat.sphinx.wrapper_common.hhmmElseDate
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class VideoFeedWatchScreenFragment: BaseFragment<
        VideoFeedWatchScreenViewState,
        VideoFeedWatchScreenViewModel,
        FragmentVideoWatchScreenBinding
        >(R.layout.fragment_video_watch_screen)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_tribe)
            .build()
    }

    override val binding: FragmentVideoWatchScreenBinding by viewBinding(FragmentVideoWatchScreenBinding::bind)
    override val viewModel: VideoFeedWatchScreenViewModel by viewModels()

    private val mHideHandler = Handler()
    private val mHideRunnable = Runnable { toggleRemoteVideoControllers() }

    private var dragging: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInsetterPadding()
        setupVideoEpisodeListAdapter()
        setupCurrentVideoEpisode()
    }

    private fun setupInsetterPadding() {
        val activity = (requireActivity() as InsetterActivity)
        binding.apply {

            constraintLayoutScrollViewContent?.let { activity.addStatusBarPadding(it) }
        }
    }

    private fun setupVideoEpisodeListAdapter() {
        binding.includeLayoutVideoEpisodesList.recyclerViewEpisodesList.apply {
            val videoFeedItemsAdapter = VideoFeedItemsAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                viewModel
            )
//            this.setHasFixedSize(false)
            adapter = videoFeedItemsAdapter
            itemAnimator = null
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.feedItemsHolderViewStateFlow.collect { feedItems ->
                binding.includeLayoutVideoEpisodesList.textViewEpisodesListCount.text = feedItems.size.toString()
            }
        }
    }

    private fun setupCurrentVideoEpisode() {
        binding.apply {
            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                viewModel.videoItemSharedFlow.collect { videoItem ->
                    videoItem?.let { videoEpisode ->
                        includeLayoutCurrentVideoDetail.apply {
                            textViewContributorName.text = videoEpisode.author?.value
                            textViewCurrentVideoDescription.text = videoEpisode.descriptionToShow
                            textViewCurrentVideoTitle.text = videoEpisode.titleToShow
                            textViewCurrentVideoPublishedDate.text = videoEpisode.datePublished?.hhmmElseDate()
                            // textViewCurrentVideoViewCount
                            videoEpisode.feed?.imageUrlToShow?.let {
                                imageLoader.load(
                                    imageView = imageViewContributorImage,
                                    it.value,
                                    imageLoaderOptions
                                )
                            }
                            if (!videoEpisode.enclosureUrl.value.contains("youtube")) { // Use proper check here...
                                constraintLayoutRemoveVideoPlayer.visible
                                layoutConstraintBottomControls.visible
                                webViewYoutubeVideoPlayer.gone

                                viewModel.videoPlayerController.setVideo(videoViewVideoPlayer)

                                videoViewVideoPlayer.setOnClickListener {
                                    toggleRemoteVideoControllers()
                                }

                                textViewPlayPauseButton.setOnClickListener {
                                    viewModel.videoPlayerController.togglePlayPause()
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
                                            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                                viewModel.videoPlayerController.seekTo(seekBar.progress)
                                            }
                                            dragging = false
                                        }
                                    }
                                )

                                viewModel.initializeVideo()
                            } else {
                                constraintLayoutRemoveVideoPlayer.gone
                                layoutConstraintBottomControls.gone
                                webViewYoutubeVideoPlayer.visible
                                webViewYoutubeVideoPlayer.settings.apply {
                                    javaScriptEnabled = true
                                }
                                webViewYoutubeVideoPlayer.loadData(
                                    "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube-nocookie.com/embed/${videoEpisode.id.value}\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>",
                                    "text/html",
                                    "utf-8"
                                )
                            }
                        }
                    }

                }


            }
        }
    }

    private fun toggleRemoteVideoControllers() {
        if (binding.includeLayoutCurrentVideoDetail.layoutConstraintBottomControls.isVisible) {
            binding.includeLayoutCurrentVideoDetail.layoutConstraintBottomControls.gone
        } else {
            binding.includeLayoutCurrentVideoDetail.layoutConstraintBottomControls.visible

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

    override suspend fun onViewStateFlowCollect(viewState: VideoFeedWatchScreenViewState) {

        when (viewState) {
            is VideoFeedWatchScreenViewState.VideoTitle -> {

            }
            is VideoFeedWatchScreenViewState.MetaDataLoaded -> {
                binding.includeLayoutCurrentVideoDetail.seekBarCurrentProgress.max = viewState.duration
                binding.includeLayoutCurrentVideoDetail.textViewCurrentTime.text = viewState.duration.toLong().toTimestamp()

//                optimizeVideoSize()
            }
            is VideoFeedWatchScreenViewState.CurrentTimeUpdate -> {
                if (!dragging) {
                    binding.includeLayoutCurrentVideoDetail.seekBarCurrentProgress.progress = viewState.currentTime
                    binding.includeLayoutCurrentVideoDetail.textViewCurrentTime.text = (viewState.duration - viewState.currentTime).toLong().toTimestamp()
                }
            }
            is VideoFeedWatchScreenViewState.ContinuePlayback -> {
                binding.includeLayoutCurrentVideoDetail.textViewPlayPauseButton.text = binding.root.context.getString(R.string.material_icon_name_pause_button)
            }
            is VideoFeedWatchScreenViewState.PausePlayback -> {
                binding.includeLayoutCurrentVideoDetail.textViewPlayPauseButton.text = binding.root.context.getString(R.string.material_icon_name_play_button)
            }
            is VideoFeedWatchScreenViewState.CompletePlayback -> {
                binding.includeLayoutCurrentVideoDetail.seekBarCurrentProgress.progress = 0
                binding.includeLayoutCurrentVideoDetail.textViewCurrentTime.text = 0L.toTimestamp()
                binding.includeLayoutCurrentVideoDetail.textViewPlayPauseButton.text = binding.root.context.getString(R.string.material_icon_name_play_button)
            }
            VideoFeedWatchScreenViewState.Idle -> { }
        }
    }

    companion object {
        /**
         * If [.AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000
    }
}
