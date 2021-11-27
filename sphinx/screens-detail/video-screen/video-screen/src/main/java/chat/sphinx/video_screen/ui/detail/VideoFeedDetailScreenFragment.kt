package chat.sphinx.video_screen.ui.detail

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.adapter.VideoFeedItemsAdapter
import chat.sphinx.video_screen.adapter.VideoFeedItemsFooterAdapter
import chat.sphinx.video_screen.databinding.FragmentVideoDetailScreenBinding
import chat.sphinx.video_screen.ui.viewstate.VideoFeedScreenViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class VideoFeedDetailScreenFragment: BaseFragment<
        VideoFeedScreenViewState,
        VideoFeedDetailScreenViewModel,
        FragmentVideoDetailScreenBinding
        >(R.layout.fragment_video_detail_screen)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

//    private val imageLoaderOptions: ImageLoaderOptions by lazy {
//        ImageLoaderOptions.Builder()
//            .placeholderResId(R.drawable.ic_tribe)
//            .build()
//    }

    override val binding: FragmentVideoDetailScreenBinding by viewBinding(FragmentVideoDetailScreenBinding::bind)
    override val viewModel: VideoFeedDetailScreenViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupItems()
    }

    private fun setupItems() {
        binding.includeLayoutVideoItemsList.recyclerViewVideoList.apply {
            val videoFeedItemsAdapter = VideoFeedItemsAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                viewModel
            )
            val videoListFooterAdapter = VideoFeedItemsFooterAdapter(requireActivity() as InsetterActivity)
            this.setHasFixedSize(false)
            adapter = ConcatAdapter(videoFeedItemsAdapter, videoListFooterAdapter)
            itemAnimator = null
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: VideoFeedScreenViewState) {
        @Exhaustive
        when (viewState) {
            is VideoFeedScreenViewState.Idle -> {}

            is VideoFeedScreenViewState.FeedLoaded -> {
                binding.apply {
                    includeLayoutVideoItemsList.textViewVideosListCount.text = viewState.items.count().toString()
                    //TODO show video feed information
                }
            }
        }
    }
}
