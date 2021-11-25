package chat.sphinx.video_screen.ui.watch

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.adapter.VideoFeedItemsAdapter
import chat.sphinx.video_screen.databinding.FragmentVideoWatchScreenBinding
import chat.sphinx.video_screen.ui.Placeholder
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
//                    val videoEpisode = viewModel.videoItemSharedFlow.lastOrNull()
                val videoEpisode = if (viewModel.args.argFeedItemId == "feedItemId") {
                    Placeholder.remoteVideoFeedItem
                } else {
                    Placeholder.youtubeFeedItem
                }
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
                        videoViewVideoPlayer.visible
                        webViewYoutubeVideoPlayer.gone

                        videoViewVideoPlayer.setOnPreparedListener {
                            it.start()
                        }
                        videoViewVideoPlayer.setVideoURI(
                            videoEpisode.enclosureUrl.value.toUri()
                        )
                    } else {
                        videoViewVideoPlayer.gone
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

    override suspend fun onViewStateFlowCollect(viewState: VideoFeedWatchScreenViewState) {
        //TODO implement view state collector
    }
}
