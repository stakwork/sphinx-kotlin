package chat.sphinx.chat_tribe.ui

import android.animation.Animator
import android.annotation.SuppressLint
import android.widget.ImageView
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.databinding.*
import chat.sphinx.chat_common.ui.ChatFragment
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.databinding.FragmentChatTribeBinding
import chat.sphinx.chat_tribe.databinding.LayoutPodcastPlayerFooterBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.resources.databinding.LayoutBoostFireworksBinding
import chat.sphinx.resources.getString
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_podcast.Podcast
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatTribeFragment: ChatFragment<
        FragmentChatTribeBinding,
        ChatTribeFragmentArgs,
        ChatTribeViewModel,
        >(R.layout.fragment_chat_tribe)
{
    private val podcastPlayerBinding: LayoutPodcastPlayerFooterBinding by viewBinding(LayoutPodcastPlayerFooterBinding::bind, R.id.include_podcast_player_footer)

    override val binding: FragmentChatTribeBinding by viewBinding(FragmentChatTribeBinding::bind)

    override val footerBinding: LayoutChatFooterBinding by viewBinding(
        LayoutChatFooterBinding::bind, R.id.include_chat_tribe_footer
    )
    override val headerBinding: LayoutChatHeaderBinding by viewBinding(
        LayoutChatHeaderBinding::bind, R.id.include_chat_tribe_header
    )
    override val replyingMessageBinding: LayoutMessageReplyBinding by viewBinding(
        LayoutMessageReplyBinding::bind, R.id.include_chat_tribe_message_reply
    )
    override val selectedMessageBinding: LayoutSelectedMessageBinding by viewBinding(
        LayoutSelectedMessageBinding::bind, R.id.include_chat_tribe_selected_message
    )
    override val selectedMessageHolderBinding: LayoutMessageHolderBinding by viewBinding(
        LayoutMessageHolderBinding::bind, R.id.include_layout_message_holder_selected_message
    )
    override val attachmentSendBinding: LayoutAttachmentSendPreviewBinding by viewBinding(
        LayoutAttachmentSendPreviewBinding::bind, R.id.include_chat_tribe_attachment_send_preview
    )

    override val attachmentFullscreenBinding: LayoutAttachmentFullscreenBinding by viewBinding(
        LayoutAttachmentFullscreenBinding::bind, R.id.include_chat_tribe_attachment_fullscreen
    )

    override val menuBinding: LayoutChatMenuBinding by viewBinding(
        LayoutChatMenuBinding::bind, R.id.include_chat_tribe_menu
    )

    override val callMenuBinding: LayoutMenuBottomBinding by viewBinding(
        LayoutMenuBottomBinding::bind, R.id.include_layout_menu_bottom_call
    )

    private val boostAnimationBinding: LayoutBoostFireworksBinding by viewBinding(
        LayoutBoostFireworksBinding::bind, R.id.include_layout_boost_fireworks
    )

    override val menuEnablePayments: Boolean
        get() = false

    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    override val viewModel: ChatTribeViewModel by viewModels()

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

    private suspend fun setupBoostAnimation(
        photoUrl: PhotoUrl?,
        amount: Sat?
    ) {
        boostAnimationBinding.apply {

            photoUrl?.let { photoUrl ->
                imageLoader.load(
                    imageViewProfilePicture,
                    photoUrl.value,
                    ImageLoaderOptions.Builder()
                        .placeholderResId(chat.sphinx.podcast_player.R.drawable.ic_profile_avatar_circle)
                        .transformation(Transformation.CircleCrop)
                        .build()
                )
            }

            textViewSatsAmount.text = amount?.asFormattedString()

            lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener{
                override fun onAnimationEnd(animation: Animator?) {
                    root.gone
                }

                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationStart(animation: Animator?) {}
            })
        }
    }

    private fun configureContributions(contributions: String) {
        headerBinding.apply {
            textViewChatHeaderContributionsIcon.visible
            textViewChatHeaderContributions.apply {
                visible
                @SuppressLint("SetTextI18n")
                text = contributions
            }
        }
    }

    private fun configurePodcastPlayer(podcast: Podcast) {
        podcastPlayerBinding.apply {
            if (root.isGone) {
                scrollToBottom(callback = {
                    root.goneIfFalse(true)
                })
            }

            togglePlayPauseButton(podcast.isPlaying)

            val currentEpisode = podcast.getCurrentEpisode()
            textViewEpisodeTitle.text = currentEpisode.title

            setProgressBar(podcast)
        }

        addPodcastOnClickListeners(podcast)
    }

    private fun setProgressBar(podcast: Podcast) {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            val progress: Int = withContext(viewModel.io) {
                try {
                    podcast.getPlayingProgress(viewModel::retrieveEpisodeDuration)
                } catch (e: ArithmeticException) {
                    0
                }
            }

            podcastPlayerBinding.apply {
                val progressWith =
                    progressBarContainer.measuredWidth.toDouble() * (progress.toDouble() / 100.0)
                progressBar.layoutParams.width = progressWith.toInt()
                progressBar.requestLayout()
            }
        }
    }

    private fun toggleLoadingWheel(show: Boolean) {
        podcastPlayerBinding.apply {
            progressBarAudioLoading.goneIfFalse(show)
        }
    }

    private fun togglePlayPauseButton(playing: Boolean) {
        podcastPlayerBinding.apply {
            textViewPlayPauseButton.text = getString(
                if (playing) R.string.material_icon_name_pause_button else R.string.material_icon_name_play_button
            )
        }
    }

    private fun addPodcastOnClickListeners(podcast: Podcast) {
        podcastPlayerBinding.apply {
            textViewEpisodeTitle.setOnClickListener {
                viewModel.goToPodcastPlayerScreen()
            }

            textViewPlayPauseButton.setOnClickListener {
                viewModel.playPausePodcast()
            }

            textViewForward30Button.setOnClickListener {
                viewModel.seekTo(30000)
                setProgressBar(podcast)
            }

            textViewBoostPodcastButton.setOnClickListener {
                viewModel.sendPodcastBoost()

                boostAnimationBinding.apply {
                    root.visible

                    lottieAnimationView.playAnimation()
                }
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.podcastViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is PodcastViewState.Idle -> {
                    }

                    is PodcastViewState.PodcastLoaded -> {
                        configurePodcastPlayer(viewState.podcast)
                    }

                    is PodcastViewState.PodcastContributionsLoaded -> {
                        configureContributions(viewState.contributions)
                    }

                    is PodcastViewState.ServiceInactive -> {
                        togglePlayPauseButton(false)
                    }

                    is PodcastViewState.ServiceLoading -> {
                        toggleLoadingWheel(true)
                    }

                    is PodcastViewState.MediaStateUpdate -> {
                        toggleLoadingWheel(false)
                        configurePodcastPlayer(viewState.podcast)
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.boostAnimationViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is BoostAnimationViewState.Idle -> {}

                    is BoostAnimationViewState.BoosAnimationInfo -> {
                        setupBoostAnimation(
                            viewState.photoUrl,
                            viewState.amount
                        )
                    }
                }
            }
        }
    }
}
