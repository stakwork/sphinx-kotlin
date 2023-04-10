package chat.sphinx.episode_description.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_connectivity_helper.ConnectivityHelper
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.create_description.R
import chat.sphinx.create_description.databinding.FragmentEpisodeDescriptionBinding
import chat.sphinx.episode_description.model.FeedItemDescription
import chat.sphinx.resources.getString
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.isPodcast
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfTrue
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class EpisodeDescriptionFragment: SideEffectFragment<
        FragmentActivity,
        EpisodeDescriptionSideEffect,
        EpisodeDescriptionViewState,
        EpisodeDescriptionViewModel,
        FragmentEpisodeDescriptionBinding
        >(R.layout.fragment_episode_description)
{
    override val viewModel: EpisodeDescriptionViewModel by viewModels()
    override val binding: FragmentEpisodeDescriptionBinding by viewBinding(FragmentEpisodeDescriptionBinding::bind)

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var connectivityHelper: ConnectivityHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BackPressHandler(viewLifecycleOwner, requireActivity())
        setAllClickListeners()
    }

    private inner class BackPressHandler(
        owner: LifecycleOwner,
        activity: FragmentActivity,
    ) : OnBackPressedCallback(true) {
        init {
            activity.apply {
                onBackPressedDispatcher.addCallback(
                    owner,
                    this@BackPressHandler,
                )
            }
        }
        override fun handleOnBackPressed() {
            viewModel.closeScreen()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: EpisodeDescriptionViewState) {
        when(viewState) {
            is EpisodeDescriptionViewState.Idle -> {}

            is EpisodeDescriptionViewState.ItemDescription -> {
                bindFeedItemDescription(viewState.feedItemDescription)
                bindItemDetailsDescription(viewState.feedItemDescription)
            }
        }
    }

    private suspend fun bindFeedItemDescription(feedItemDescription: FeedItemDescription) {
        binding.apply {
            textViewItemTitle.text = feedItemDescription.feedItemTitle
            textViewFeedTitle.text = feedItemDescription.feedTitle
            textViewDescriptionEpisode.text = feedItemDescription.description

            if (feedItemDescription.descriptionExpanded) {
                textViewDescriptionEpisode.maxLines = Int.MAX_VALUE
                textViewShowMore.text = getString(R.string.episode_description_show_less)
            } else {
                textViewDescriptionEpisode.maxLines = 5
                textViewShowMore.text = getString(R.string.episode_description_show_more)
            }

            imageViewItemRowEpisodeType.setImageDrawable(
                ContextCompat.getDrawable(
                    root.context,
                    getFeedItemDrawableType(feedItemDescription.feedType)
                )
            )

            imageLoader.load(
                imageViewEpisodeDetailImage,
                feedItemDescription.image,
                ImageLoaderOptions.Builder()
                    .placeholderResId(getFeedItemPlaceholderDrawable(feedItemDescription.feedType))
                    .build()
            )

            textViewEpisodeDate.text = feedItemDescription.itemDate
            textViewItemEpisodeTime.text = feedItemDescription.itemDuration

            if (feedItemDescription.played) {
                buttonCheckMarkPlayed.visible
                textViewItemEpisodeTime.text = getString(R.string.episode_detail_played_holder)
            } else {
                buttonCheckMarkPlayed.gone
            }

            if (feedItemDescription.downloaded) {
                imageDownloadedEpisodeArrow.visible
                buttonDownloadArrow.gone
                progressBarEpisodeDownload.gone
                buttonStop.gone
            } else {
                buttonDownloadArrow.visible
                buttonStop.gone
                progressBarEpisodeDownload.gone
                imageDownloadedEpisodeArrow.gone
            }

            val isFeedItemDownloadInProgress = feedItemDescription.downloading && !feedItemDescription.downloaded

            if (isFeedItemDownloadInProgress) {
                buttonDownloadArrow.gone
                progressBarEpisodeDownload.visible
                imageDownloadedEpisodeArrow.gone
                buttonStop.visible
            }

            if (feedItemDescription.playing) {
                buttonPlayEpisode.setImageDrawable(
                    ContextCompat.getDrawable(binding.root.context, R.drawable.ic_pause_episode)
                )
            } else {
                buttonPlayEpisode.setImageDrawable(
                    ContextCompat.getDrawable(binding.root.context, R.drawable.ic_play_episode)
                )
            }
            if (feedItemDescription.feedType?.isPodcast() == true) {
                buttonPlayEpisode.visible
                textViewItemEpisodeTime.visible
                circleSplit.visible

                buttonDownloadArrow.alpha = 1.0F
                buttonDownloadArrow.isEnabled = true
            } else {
                buttonPlayEpisode.gone
                textViewItemEpisodeTime.gone
                circleSplit.gone

                buttonDownloadArrow.alpha = 0.3F
                buttonDownloadArrow.isEnabled = false
            }
        }
    }

    private fun bindItemDetailsDescription(feedItemDescription: FeedItemDescription) {
        feedItemDetailsCommonInfoBinding(feedItemDescription)
        setFeedItemDetailsDownloadState(feedItemDescription)
        setPlayedMarkState(feedItemDescription)
    }

    private fun feedItemDetailsCommonInfoBinding(feedItemDescription: FeedItemDescription) {
        binding.includeLayoutFeedItem.includeLayoutFeedItemDetails.apply {

            if (feedItemDescription.feedType?.isPodcast() == true) {
                layoutConstraintDownloadRow.visible
                layoutConstraintCheckMarkRow.visible
                circleSplitTwo.visible
                textViewEpisodeDuration.visible
            } else {
                layoutConstraintDownloadRow.gone
                layoutConstraintCheckMarkRow.gone
                circleSplitTwo.gone
                textViewEpisodeDuration.gone
            }

            textViewMainEpisodeTitle.text = feedItemDescription.feedItemTitle
            textViewEpisodeType.text = getFeedItemStringType(feedItemDescription.feedType)
            textViewEpisodeDate.text = feedItemDescription.itemDate
            textViewEpisodeDuration.text = feedItemDescription.itemDuration
            textViewPodcastName.text = feedItemDescription.feedTitle

            imageViewItemRowEpisodeType.setImageDrawable(
                ContextCompat.getDrawable(
                    root.context,
                    getFeedItemDrawableType(feedItemDescription.feedType)
                )
            )

            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                imageLoader.load(
                    imageViewEpisodeDetailImage,
                    feedItemDescription.image,
                    ImageLoaderOptions.Builder()
                        .placeholderResId(getFeedItemPlaceholderDrawable(feedItemDescription.feedType))
                        .build()
                )
            }
        }
    }
    private fun setPlayedMarkState(feedItemDescription: FeedItemDescription) {
        binding.includeLayoutFeedItem.includeLayoutFeedItemDetails.apply {
            if (feedItemDescription.played) {
                buttonCheckMarkPlayed.visible
                buttonCheckMark.invisible
                textViewCheckMark.text = getString(R.string.episode_detail_upplayed)
            } else {
                buttonCheckMarkPlayed.invisible
                buttonCheckMark.visible
                textViewCheckMark.text = getString(R.string.episode_detail_played)
            }
        }
    }

    private fun setFeedItemDetailsDownloadState(feedItemDescription: FeedItemDescription) {
        binding.includeLayoutFeedItem.includeLayoutFeedItemDetails.apply {
            if (feedItemDescription.downloading) {
                buttonDownloadArrow.gone
                imageDownloadedEpisodeArrow.gone
                progressBarEpisodeDownload.visible
                buttonStop.visible
            } else {
                if (feedItemDescription.downloaded) {
                    buttonDownloadArrow.gone
                    imageDownloadedEpisodeArrow.visible
                    progressBarEpisodeDownload.gone
                    buttonStop.gone
                    textViewDownload.text = getString(R.string.episode_detail_erase)
                } else {
                    buttonDownloadArrow.visible
                    imageDownloadedEpisodeArrow.gone
                    progressBarEpisodeDownload.gone
                    buttonStop.gone
                    textViewDownload.text = getString(R.string.episode_detail_download)
                }
            }
        }
    }

    private fun setAllClickListeners() {
        binding.apply {

            buttonNavBack.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }

            constraintShowMoreContainer.setOnClickListener {
                toggleShowMore()
            }

            buttonEpisodeShare.setOnClickListener {
                viewModel.share(
                    binding.root.context,
                    getString(R.string.episode_detail_share_link)
                )
            }

            buttonDownloadArrow.setOnClickListener {
                viewModel.downloadMedia()
            }

            buttonPlayEpisode.setOnClickListener {
                if (connectivityHelper.isNetworkConnected()) {
                    viewModel.togglePlayState()
                }
            }

            buttonAdditionalOptions.setOnClickListener {
                viewModel.openDetailScreen()
            }

            includeLayoutFeedItem.includeLayoutFeedItemDetails.apply {
                layoutConstraintCloseContainer.setOnClickListener {
                    viewModel.closeScreen()
                }

                layoutConstraintCheckMarkRow.setOnClickListener {
                    viewModel.updatePlayedMark()
                }

                layoutConstraintShareRow.setOnClickListener {
                    viewModel.share(
                        binding.root.context,
                        getString(R.string.episode_detail_share_link)
                    )
                }

                layoutConstraintCopyLinkRow.setOnClickListener {
                    viewModel.copyCodeToClipboard()
                }

                layoutConstraintDownloadRow.setOnClickListener {
                    viewModel.toggleDownloadState()
                }
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.feedItemDetailsMenuViewStateContainer.collect { viewState ->

                binding.includeLayoutFeedItem.apply {
                    root.setTransitionDuration(300)
                    viewState.transitionToEndSet(root)
                }
            }
        }
    }

    private fun toggleShowMore() {
        viewModel.toggleDescriptionExpanded()
    }

    override suspend fun onSideEffectCollect(sideEffect: EpisodeDescriptionSideEffect) {
        sideEffect.execute(requireActivity())
    }

    private fun getFeedItemDrawableType(feedType: FeedType?): Int {
        return when (feedType) {
            is FeedType.Podcast -> R.drawable.ic_podcast_type
            is FeedType.Video -> R.drawable.ic_youtube_type
            else -> R.drawable.ic_podcast_placeholder
        }
    }

    private fun getFeedItemPlaceholderDrawable(feedType: FeedType?): Int {
        return when (feedType) {
            is FeedType.Podcast -> R.drawable.ic_podcast_placeholder
            is FeedType.Video -> R.drawable.ic_video_placeholder
            is FeedType.Newsletter -> R.drawable.ic_newsletter_placeholder
            else -> R.drawable.ic_podcast_placeholder
        }
    }

    private fun getFeedItemStringType(feedType: FeedType?): String {
        return when (feedType) {
            is FeedType.Podcast -> "Podcast"
            is FeedType.Video -> "Youtube"
            else -> ""
        }
    }

}
