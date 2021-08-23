package chat.sphinx.chat_tribe.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.databinding.*
import chat.sphinx.chat_common.ui.ChatFragment
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.databinding.FragmentChatTribeBinding
import chat.sphinx.chat_tribe.databinding.LayoutPodcastPlayerFooterBinding
import chat.sphinx.chat_tribe.model.TribePodcastData
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.resources.databinding.LayoutBoostFireworksBinding
import chat.sphinx.resources.getString
import chat.sphinx.wrapper_view.Px
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatTribeFragment: ChatFragment<
        FragmentChatTribeBinding,
        ChatTribeFragmentArgs,
        ChatTribeViewModel,
        >(R.layout.fragment_chat_tribe)
{
    override val binding: FragmentChatTribeBinding by viewBinding(FragmentChatTribeBinding::bind)
    private val podcastPlayerBinding: LayoutPodcastPlayerFooterBinding
        get() = binding.includePodcastPlayerFooter
    private val boostAnimationBinding: LayoutBoostFireworksBinding
        get() = binding.includeLayoutBoostFireworks

    override val footerBinding: LayoutChatFooterBinding
        get() = binding.includeChatTribeFooter
    override val headerBinding: LayoutChatHeaderBinding
        get() = binding.includeChatTribeHeader
    override val replyingMessageBinding: LayoutMessageReplyBinding
        get() = binding.includeChatTribeMessageReply
    override val selectedMessageBinding: LayoutSelectedMessageBinding
        get() = binding.includeChatTribeSelectedMessage
    override val selectedMessageHolderBinding: LayoutMessageHolderBinding
        get() = binding.includeChatTribeSelectedMessage.includeLayoutMessageHolderSelectedMessage
    override val attachmentSendBinding: LayoutAttachmentSendPreviewBinding
        get() = binding.includeChatTribeAttachmentSendPreview
    override val menuBinding: LayoutChatMenuBinding
        get() = binding.includeChatTribeMenu
    override val callMenuBinding: LayoutMenuBottomBinding
        get() = binding.includeLayoutMenuBottomCall
    override val attachmentFullscreenBinding: LayoutAttachmentFullscreenBinding
        get() = binding.includeChatTribeAttachmentFullscreen

    override val menuEnablePayments: Boolean
        get() = false

    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    override val viewModel: ChatTribeViewModel by viewModels()
    private val podcastViewModel: PodcastViewModel by viewModels()

    @Inject
    @Suppress("ProtectedInFinal", "PropertyName")
    protected lateinit var _userColorsHelper: UserColorsHelper
    override val userColorsHelper: UserColorsHelper
        get() = _userColorsHelper

    @Inject
    @Suppress("ProtectedInFinal", "PropertyName")
    protected lateinit var _imageLoader: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = _imageLoader

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(viewModel.mainImmediate) {
            try {
                viewModel.podcastDataStateFlow.collect { data ->
                    @Exhaustive
                    when (data) {
                        is TribePodcastData.Loading -> {}
                        is TribePodcastData.Result -> {
                            podcastViewModel.init(data)
                            throw Exception()
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        podcastPlayerBinding.apply {
            textViewBoostPodcastButton.setOnClickListener {
                podcastViewModel.currentViewState.clickBoost?.invoke()
            }
            textViewForward30Button.setOnClickListener {
                podcastViewModel.currentViewState.clickFastForward?.invoke()
            }
            textViewPlayPauseButton.setOnClickListener {
                podcastViewModel.currentViewState.clickPlayPause?.invoke()
            }
            root.setOnClickListener {
                podcastViewModel.currentViewState.clickTitle?.invoke()
            }
        }
    }

//    private suspend fun setupBoostAnimation(
//        photoUrl: PhotoUrl?,
//        amount: Sat?
//    ) {
//        boostAnimationBinding.apply {
//
//            photoUrl?.let { photoUrl ->
//                imageLoader.load(
//                    imageViewProfilePicture,
//                    photoUrl.value,
//                    ImageLoaderOptions.Builder()
//                        .placeholderResId(chat.sphinx.podcast_player.R.drawable.ic_profile_avatar_circle)
//                        .transformation(Transformation.CircleCrop)
//                        .build()
//                )
//            }
//
//            textViewSatsAmount.text = amount?.asFormattedString()
//
//            lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener{
//                override fun onAnimationEnd(animation: Animator?) {
//                    root.gone
//                }
//
//                override fun onAnimationRepeat(animation: Animator?) {}
//
//                override fun onAnimationCancel(animation: Animator?) {}
//
//                override fun onAnimationStart(animation: Animator?) {}
//            })
//        }
//    }

//    private fun configureContributions(contributions: String) {
//        headerBinding.apply {
//            textViewChatHeaderContributionsIcon.visible
//            textViewChatHeaderContributions.apply {
//                visible
//                @SuppressLint("SetTextI18n")
//                text = contributions
//            }
//        }
//    }
//
//    private fun configurePodcastPlayer(podcast: Podcast) {
//        podcastPlayerBinding.apply {
//            if (root.isGone) {
//                scrollToBottom(callback = {
//                    root.goneIfFalse(true)
//                })
//            }
//
//            togglePlayPauseButton(podcast.isPlaying)
//
//            val currentEpisode = podcast.getCurrentEpisode()
//            textViewEpisodeTitle.text = currentEpisode.title
//
//            setProgressBar(podcast)
//        }
//
//        addPodcastOnClickListeners(podcast)
//    }
//
//    private fun setProgressBar(podcast: Podcast) {
//        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
//            val progress: Int = withContext(viewModel.io) {
//                try {
//                    podcast.getPlayingProgress(viewModel::retrieveEpisodeDuration)
//                } catch (e: ArithmeticException) {
//                    0
//                }
//            }
//
//            podcastPlayerBinding.apply {
//                val progressWith =
//                    progressBarContainer.measuredWidth.toDouble() * (progress.toDouble() / 100.0)
//                progressBar.layoutParams.width = progressWith.toInt()
//                progressBar.requestLayout()
//            }
//        }
//    }
//
//    private fun toggleLoadingWheel(show: Boolean) {
//        podcastPlayerBinding.apply {
//            progressBarAudioLoading.goneIfFalse(show)
//        }
//    }
//
//    private fun togglePlayPauseButton(playing: Boolean) {
//        podcastPlayerBinding.apply {
//            textViewPlayPauseButton.text = getString(
//                if (playing) R.string.material_icon_name_pause_button else R.string.material_icon_name_play_button
//            )
//        }
//    }
//
//    private fun addPodcastOnClickListeners(podcast: Podcast) {
//        podcastPlayerBinding.apply {
//            textViewEpisodeTitle.setOnClickListener {
//                viewModel.goToPodcastPlayerScreen()
//            }
//
//            textViewPlayPauseButton.setOnClickListener {
//                viewModel.playPausePodcast()
//            }
//
//            textViewForward30Button.setOnClickListener {
//                viewModel.seekTo(30000)
//                setProgressBar(podcast)
//            }
//
//            textViewBoostPodcastButton.setOnClickListener {
//                viewModel.sendPodcastBoost()
//
//                boostAnimationBinding.apply {
//                    root.visible
//
//                    lottieAnimationView.playAnimation()
//                }
//            }
//        }
//    }

    private val progressWidth: Px by lazy {
        Px(binding.root.measuredWidth.toFloat())
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

//        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
//            viewModel.podcastViewStateContainer.collect { viewState ->
//                @Exhaustive
//                when (viewState) {
//                    is PodcastViewState.Idle -> {
//                    }
//
//                    is PodcastViewState.PodcastLoaded -> {
//                        configurePodcastPlayer(viewState.podcast)
//                    }
//
//                    is PodcastViewState.PodcastContributionsLoaded -> {
//                        configureContributions(viewState.contributions)
//                    }
//
//                    is PodcastViewState.ServiceInactive -> {
//                        togglePlayPauseButton(false)
//                    }
//
//                    is PodcastViewState.ServiceLoading -> {
//                        toggleLoadingWheel(true)
//                    }
//
//                    is PodcastViewState.MediaStateUpdate -> {
//                        toggleLoadingWheel(false)
//                        configurePodcastPlayer(viewState.podcast)
//                    }
//                }
//            }
//        }

//        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
//            viewModel.boostAnimationViewStateContainer.collect { viewState ->
//                @Exhaustive
//                when (viewState) {
//                    is BoostAnimationViewState.Idle -> {}
//
//                    is BoostAnimationViewState.BoosAnimationInfo -> {
//                        setupBoostAnimation(
//                            viewState.photoUrl,
//                            viewState.amount
//                        )
//                    }
//                }
//            }
//        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            podcastViewModel.collectViewState { viewState ->
                podcastPlayerBinding.apply {
                    when (viewState) {
                        is PodcastViewState2.Available -> {

                            textViewPlayPauseButton.text = if (viewState.showPlayButton) {
                                getString(R.string.material_icon_name_play_button)
                            } else {
                                getString(R.string.material_icon_name_pause_button)
                            }

                            val calculatedWidth = progressWidth.value.toDouble() * (viewState.playingProgress / 100.0)
                            progressBar.layoutParams.width = calculatedWidth.toInt()
                            progressBar.requestLayout()

                            textViewEpisodeTitle.text = viewState.title

                            if (viewState.showLoading) {
                                progressBarAudioLoading.visible
                            } else {
                                progressBarAudioLoading.gone
                            }

                            root.visible
                        }
                        is PodcastViewState2.NoPodcast -> {
                            root.gone
                        }
                    }
                }
            }
        }
    }
}
