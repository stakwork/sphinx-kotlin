package chat.sphinx.episode_description.ui

import android.content.Context
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
import chat.sphinx.episode_description.model.EpisodeDescription
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfTrue
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.concept_views.viewstate.value
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
            is EpisodeDescriptionViewState.FeedItemDescription -> {
                bindFeedItemDescription(viewState)
            }
            is EpisodeDescriptionViewState.FeedItemDetails -> {

                binding.includeLayoutFeedItem.apply {
                    root.setTransitionDuration(300)
                    viewState.feedItemDescription.transitionToEndSet(root)

                    if (viewState.feedItemDescription is FeedItemDescriptionDetailsViewState.Open) {
                        feedItemDetailsCommonInfoBinding(viewState.feedItemDescription)
                        setFeedItemDetailsDownloadState(viewState.feedItemDescription)
                        setPlayedMarkState(viewState.feedItemDescription)
                    }
                }
            }
        }
    }

    private suspend fun bindFeedItemDescription(viewState: EpisodeDescriptionViewState.FeedItemDescription) {
        binding.apply {
            textViewTitleHeader.text = viewState.feedItemDescription.header
            textViewDescriptionEpisode.text = viewState.feedItemDescription.description
            textViewDescriptionEpisodeTitle.text = viewState.feedItemDescription.feedName
            imageViewItemRowEpisodeType.setImageDrawable((ContextCompat.getDrawable(root.context, viewState.feedItemDescription.episodeTypeImage)))
            imageLoader.load(
                imageViewEpisodeDetailImage,
                viewState.feedItemDescription.image,
                ImageLoaderOptions.Builder()
                    .placeholderResId(R.drawable.ic_podcast_placeholder)
                    .build()
            )
            textViewEpisodeDate.text = viewState.feedItemDescription.episodeDate
            textViewItemEpisodeTime.text = viewState.feedItemDescription.episodeDuration
            binding.textViewDescriptionEpisode.post {
                val numberOfLines = textViewDescriptionEpisode.lineCount
                binding.constraintShowMoreContainer.goneIfTrue(numberOfLines < 5)
            }

            if (viewState.feedItemDescription.downloaded == true) {
                imageDownloadedEpisodeArrow.visible
                buttonDownloadArrow.gone
                progressBarEpisodeDownload.gone
                buttonStop.gone
            }

            if (viewState.feedItemDescription.downloaded == false) {
                buttonDownloadArrow.visible
                buttonStop.gone
                progressBarEpisodeDownload.gone
                imageDownloadedEpisodeArrow.gone
            }

            val isFeedItemDownloadInProgress = viewState.feedItemDescription.isDownloadInProgress == true && viewState.feedItemDescription.downloaded == false

            if (isFeedItemDownloadInProgress) {
                buttonDownloadArrow.gone
                progressBarEpisodeDownload.visible
                imageDownloadedEpisodeArrow.gone
                buttonStop.visible
            }

            if (viewState.feedItemDescription.isEpisodeSoundPlaying == true) {
                buttonPlayEpisode.setImageDrawable(
                    ContextCompat.getDrawable(binding.root.context, R.drawable.ic_pause_episode)
                )
            } else {
                buttonPlayEpisode.setImageDrawable(
                    ContextCompat.getDrawable(binding.root.context, R.drawable.ic_play_episode)
                )
            }
        }
    }

    private fun feedItemDetailsCommonInfoBinding(viewState: FeedItemDescriptionDetailsViewState.Open) {
        binding.includeLayoutFeedItem.includeLayoutFeedItemDetails.apply {
            textViewMainEpisodeTitle.text = viewState.feedItemDetail.header
            imageViewItemRowEpisodeType.setImageDrawable(ContextCompat.getDrawable(root.context, viewState.feedItemDetail.episodeTypeImage))
            textViewEpisodeType.text = viewState.feedItemDetail.episodeTypeText
            textViewEpisodeDate.text = viewState.feedItemDetail.episodeDate
            textViewEpisodeDuration.text = viewState.feedItemDetail.episodeDuration
            textViewPodcastName.text = viewState.feedItemDetail.feedName

            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                viewState.feedItemDetail.image.let {
                    imageLoader.load(
                        imageViewEpisodeDetailImage,
                        it,
                        ImageLoaderOptions.Builder()
                            .placeholderResId(R.drawable.ic_podcast_placeholder)
                            .build()
                    )
                }
            }
        }
    }
    private fun setPlayedMarkState(viewState: FeedItemDescriptionDetailsViewState.Open) {
        binding.includeLayoutFeedItem.includeLayoutFeedItemDetails.apply {
            if (viewState.feedItemDetail.played) {
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

    private fun setFeedItemDetailsDownloadState(viewState: FeedItemDescriptionDetailsViewState.Open) {
        binding.includeLayoutFeedItem.includeLayoutFeedItemDetails.apply {
            if (viewState.feedItemDetail.isDownloadInProgress == true) {
                buttonDownloadArrow.gone
                imageDownloadedEpisodeArrow.gone
                progressBarEpisodeDownload.visible
                buttonStop.visible
            } else {
                if (viewState.feedItemDetail.downloaded == true) {
                    buttonDownloadArrow.gone
                    imageDownloadedEpisodeArrow.visible
                    progressBarEpisodeDownload.gone
                    buttonStop.gone
                    textViewDownload.text = getString(R.string.episode_detail_erase)
                }
                else {
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
                viewModel.share(binding.root.context, getString(R.string.episode_detail_clipboard))
            }
            buttonDownloadArrow.setOnClickListener {
                viewModel.downloadMedia()
            }
            buttonPlayEpisode.setOnClickListener {
                if (connectivityHelper.isNetworkConnected()) {
                    viewModel.playEpisodeFromDescription()
                }
            }
            buttonAdditionalOptions.setOnClickListener {
                viewModel.openDetailScreen()
            }

            includeLayoutFeedItem.includeLayoutFeedItemDetails.apply {
                layoutConstraintCloseContainer.setOnClickListener {
                viewModel.closeScreen()
            }
                layoutConstraintDownloadRow.setOnClickListener {
                    (viewModel.currentViewState as? EpisodeDescriptionViewState.FeedItemDetails)?.let { viewState ->
                        if (viewState.feedItemDescription is FeedItemDescriptionDetailsViewState.Open) {
                            if (viewState.feedItemDescription.feedItemDetail.downloaded == true) {
                                viewModel.deleteDownloadedMediaByItemId()
                            } else {
                                viewModel.downloadMedia()
                            }
                        }
                    }
                }
                layoutConstraintCheckMarkRow.setOnClickListener {
                    viewModel.updatePlayedMark()
                }
                layoutConstraintShareRow.setOnClickListener {
                    viewModel.share(binding.root.context, getString(R.string.episode_detail_clipboard))
                }
                layoutConstraintCopyLinkRow.setOnClickListener {
                    viewModel.copyCodeToClipboard()
                }
            }
        }
    }

    private fun toggleShowMore() {
        binding.apply {
            if (textViewDescriptionEpisode.maxLines > 5) {
                textViewDescriptionEpisode.maxLines = 5
                textViewShowMore.text = getString(R.string.episode_description_show_more)
            }
            else {
                textViewDescriptionEpisode.maxLines = Int.MAX_VALUE
                textViewShowMore.text = getString(R.string.episode_description_show_less)
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: EpisodeDescriptionSideEffect) {
        sideEffect.execute(requireActivity())
    }


}
