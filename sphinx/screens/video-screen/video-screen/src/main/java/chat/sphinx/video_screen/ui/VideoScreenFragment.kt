package chat.sphinx.video_screen.ui

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
import chat.sphinx.video_screen.databinding.FragmentVideoScreenBinding
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_feed.*
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.goneIfTrue
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.sql.Date
import javax.inject.Inject

@AndroidEntryPoint
internal class VideoScreenFragment: BaseFragment<
        VideoScreenViewState,
        VideoScreenViewModel,
        FragmentVideoScreenBinding
        >(R.layout.fragment_video_screen)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_tribe)
            .build()
    }

    override val binding: FragmentVideoScreenBinding by viewBinding(FragmentVideoScreenBinding::bind)
    override val viewModel: VideoScreenViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInsetterPadding()
        setupVideoFeedDetail()
        setupVideoEpisodeListAdapter()
        setupCurrentVideoEpisode()
    }

    private fun setupInsetterPadding() {
        val activity = (requireActivity() as InsetterActivity)
        binding.apply {

            activity.addStatusBarPadding(constraintLayoutScrollViewContent)
        }
    }

    private fun setupVideoFeedDetail() {
        binding.includeLayoutVideoFeedDetails.apply {

            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
//                val videoFeed = viewModel.videoFeedSharedFlow.last()
                val videoFeed = if (viewModel.args.argFeedId == "youtubeFeedId") {
                    Feed(
                        FeedId("feedYoutubeItemId"),
                        FeedType.Video,
                        FeedTitle("Youtube we see a lot"),
                        FeedDescription("Describing the things we see"),
                        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
                        FeedAuthor("Youtube Channel"),
                        null,
                        PhotoUrl("https://cdn.mos.cms.futurecdn.net/8gzcr6RpGStvZFA2qRt4v6.jpg"),
                        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
                        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
                        DateTime(Date.valueOf("2021-09-22")),
                        DateTime(Date.valueOf("2021-09-22")),
                        null,
                        null,
                        FeedItemsCount(0L),
                        null,
                        ChatId(0L),
                    )
                } else {
                    Feed(
                        FeedId("feedItemId"),
                        FeedType.Video,
                        FeedTitle("Youtube we see a lot"),
                        FeedDescription("Describing the things we see"),
                        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
                        FeedAuthor("Normal Video Feed"),
                        null,
                        PhotoUrl("https://pbs.twimg.com/media/FEvdQm5XoAAcXgw?format=jpg&name=small"),
                        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
                        FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
                        DateTime(Date.valueOf("2021-09-22")),
                        DateTime(Date.valueOf("2021-09-22")),
                        null,
                        null,
                        FeedItemsCount(0L),
                        null,
                        ChatId(0L),
                    )
                }

                videoFeed?.let { videoFeed ->
                    textViewCurrentVideoDescription.text = videoFeed.descriptionToShow
                    textViewContributorName.text = videoFeed.author?.value
                    videoFeed.imageUrl?.let {
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

    private fun setupVideoEpisodeListAdapter() {
        binding.includeLayoutVideoEpisodesList.recyclerViewEpisodesList.apply {
            val videoFeedItemsAdapter = VideoFeedItemsAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                viewModel
            )
            this.setHasFixedSize(false)
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
            includeLayoutCurrentVideoDetail.root.goneIfTrue(viewModel.args.argFeedItemId == null)
            includeLayoutVideoFeedDetails.root.goneIfFalse(viewModel.args.argFeedItemId == null)

            viewModel.args.argFeedItemId?.let {
                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
//                    val videoEpisode = viewModel.videoItemSharedFlow.lastOrNull()
                    val videoEpisode = if (viewModel.args.argFeedItemId == "feedItemId") {
                        FeedItem(
                            FeedId("feedItemId"),
                            FeedTitle("Something we see a lot"),
                            FeedDescription("Describing the things we see"),
                            DateTime(Date.valueOf("2021-09-22")),
                            DateTime(Date.valueOf("2021-09-22")),
                            FeedAuthor("Kgothatso"),
                            null,
                            null,
                            FeedUrl("https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4"),
                            null,
                            PhotoUrl("https://pbs.twimg.com/media/FEvdQm5XoAAcXgw?format=jpg&name=small"),
                            PhotoUrl("https://pbs.twimg.com/media/FEvdQm5XoAAcXgw?format=jpg&name=small"),
                            FeedUrl("https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4"),
                            FeedId("feedId"),
                        )
                    } else {
                        FeedItem(
                            FeedId("jNQXAC9IVRw"),
                            FeedTitle("Youtube we see a lot"),
                            FeedDescription("Describing the things we see"),
                            DateTime(Date.valueOf("2021-09-22")),
                            DateTime(Date.valueOf("2021-09-22")),
                            FeedAuthor("Youtube Channel"),
                            null,
                            null,
                            FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
                            null,
                            PhotoUrl("https://cdn.mos.cms.futurecdn.net/8gzcr6RpGStvZFA2qRt4v6.jpg"),
                            PhotoUrl("https://cdn.mos.cms.futurecdn.net/8gzcr6RpGStvZFA2qRt4v6.jpg"),
                            FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
                            FeedId("youtubeFeedId"),
                        )
                    }
                    includeLayoutCurrentVideoDetail.apply {
                        textViewContributorName.text = videoEpisode.author?.value
                        textViewCurrentVideoDescription.text = videoEpisode.descriptionToShow
                        textViewCurrentVideoTitle.text = videoEpisode.titleToShow
                        textViewCurrentVideoPublishedDate.text = videoEpisode.datePublished?.hhmmElseDate()
                        // textViewCurrentVideoViewCount
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


    }
    override suspend fun onViewStateFlowCollect(viewState: VideoScreenViewState) {
        //TODO implement view state collector
    }
}
