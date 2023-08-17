package chat.sphinx.episode_detail.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_connectivity_helper.ConnectivityHelper
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.episode_detail.R
import chat.sphinx.episode_detail.databinding.FragmentEpisodeDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
internal class EpisodeDetailFragment: SideEffectFragment<
        Context,
        EpisodeDetailSideEffect,
        EpisodeDetailViewState,
        EpisodeDetailViewModel,
        FragmentEpisodeDetailBinding
        >(R.layout.fragment_episode_detail)
{
    override val viewModel: EpisodeDetailViewModel by viewModels()
    override val binding: FragmentEpisodeDetailBinding by viewBinding(FragmentEpisodeDetailBinding::bind)

    companion object {
        const val YOUTUBE_TYPE = "Youtube"
        const val PODCAST_TYPE = "Podcast"
    }

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var connectivityHelper: ConnectivityHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setClickListeners()
    }

    override suspend fun onViewStateFlowCollect(viewState: EpisodeDetailViewState) {
        when(viewState) {
            is EpisodeDetailViewState.Idle -> {}
            is EpisodeDetailViewState.ShowEpisode -> {
                commonInfoBinding(viewState)
                binding.apply {
                    when(viewState.episodeDetail.episodeTypeText) {
                        PODCAST_TYPE -> {
                            setDownloadState(viewState)
                            setPlayedMarkState(viewState)
                        }
                        YOUTUBE_TYPE -> {
                            layoutConstraintDownloadRow.gone
                            layoutConstraintCheckMarkRow.gone
                            circleSplitTwo.gone
                            textViewEpisodeDuration.gone
                        }
                    }
                }
            }
        }
    }

    private fun commonInfoBinding(viewState: EpisodeDetailViewState.ShowEpisode) {
        binding.apply {
            textViewMainEpisodeTitle.text = viewState.episodeDetail.header
            imageViewItemRowEpisodeType.setImageDrawable(ContextCompat.getDrawable(root.context, viewState.episodeDetail.episodeTypeImage))
            textViewEpisodeType.text = viewState.episodeDetail.episodeTypeText
            textViewEpisodeDate.text = viewState.episodeDetail.episodeDate
            textViewEpisodeDuration.text = viewState.episodeDetail.episodeDuration

            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                imageLoader.load(
                    imageViewEpisodeDetailImage,
                    viewState.episodeDetail.image,
                    ImageLoaderOptions.Builder()
                        .placeholderResId(R.drawable.ic_podcast_placeholder)
                        .build()
                )
            }
        }
    }

    private fun setDownloadState(viewState: EpisodeDetailViewState.ShowEpisode) {
        binding.apply {
            if (viewState.episodeDetail.isDownloadInProgress == true) {
                buttonDownloadArrow.gone
                imageDownloadedEpisodeArrow.gone
                progressBarEpisodeDownload.visible
                buttonStop.visible
            } else {
                if (viewState.episodeDetail.downloaded == true) {
                    buttonDownloadArrow.gone
                    imageDownloadedEpisodeArrow.visible
                    progressBarEpisodeDownload.gone
                    buttonStop.gone
                    textViewDownload.text = getString(R.string.episode_detail_erase)

                    binding.layoutConstraintDownloadRow.setOnClickListener {
                        viewModel.deleteDownloadedMedia()
                    }
                }
                else {
                    buttonDownloadArrow.visible
                    imageDownloadedEpisodeArrow.gone
                    progressBarEpisodeDownload.gone
                    buttonStop.gone
                    textViewDownload.text = getString(R.string.episode_detail_download)

                    binding.layoutConstraintDownloadRow.setOnClickListener {
                        viewModel.downloadMedia()
                    }
                }
            }
        }
    }

    private fun setPlayedMarkState(viewState: EpisodeDetailViewState.ShowEpisode) {
        binding.apply {
            layoutConstraintCheckMarkRow.setOnClickListener {
                viewModel.updatePlayedMark(!viewState.episodeDetail.played)
            }
            if (viewState.episodeDetail.played) {
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

    private fun setClickListeners() {
        binding.layoutConstraintCloseContainer.setOnClickListener {
            viewModel.popBackStack()
        }
        binding.layoutConstraintCopyLinkRow.setOnClickListener {
            viewModel.copyCodeToClipboard()
        }
        binding.layoutConstraintShareRow.setOnClickListener {
            binding.root.context.startActivity(viewModel.shareCodeThroughTextIntent())
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: EpisodeDetailSideEffect) {
        sideEffect.execute(binding.root.context)
    }


}
