package chat.sphinx.chat_tribe.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.view.isGone
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
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.resources.getString
import chat.sphinx.wrapper_podcast.Podcast
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.flow.collect
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
    override val menuBinding: LayoutChatMenuBinding by viewBinding(
        LayoutChatMenuBinding::bind, R.id.include_chat_tribe_menu
    )

    override val memberRemovalBinding: LayoutMessageTypeGroupActionMemberRemovalBinding by viewBinding(
        LayoutMessageTypeGroupActionMemberRemovalBinding::bind, R.id.include_message_type_group_action_member_removal
    )

    override val menuEnablePayments: Boolean
        get() = false

    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    override val viewModel: ChatTribeViewModel by viewModels()

    @Inject
    protected lateinit var imageLoaderInj: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.loadTribeAndPodcastData()?.let { podcast ->
                configurePodcastPlayer(podcast)
                configureContributions()
                addPodcastOnClickListeners(podcast)
            }
        }
    }

    private fun configureContributions() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.getPodcastContributionsString().collect { contributionsString ->

                headerBinding.apply {
                    textViewChatHeaderContributionsIcon.visible
                    textViewChatHeaderContributions.apply {
                        visible
                        @SuppressLint("SetTextI18n")
                        text = contributionsString
                    }
                }
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
                //TODO: Boost podcast episode
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.podcastViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is PodcastViewState.Idle -> {}

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
    }
}
