package chat.sphinx.chat_tribe.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.motion.widget.MotionLayout
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
import chat.sphinx.chat_common.databinding.LayoutPodcastPlayerFooterBinding
import chat.sphinx.chat_common.ui.viewstate.ActionsMenuViewState
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.podcast_player.objects.Podcast
import dagger.hilt.android.AndroidEntryPoint
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
        LayoutChatFooterBinding::bind, R.id.include_chat_footer
    )
    override val headerBinding: LayoutChatHeaderBinding by viewBinding(
        LayoutChatHeaderBinding::bind, R.id.include_chat_header
    )
    override val menuBinding: LayoutChatActionsMenuBinding by viewBinding(
        LayoutChatActionsMenuBinding::bind, R.id.include_chat_actions_menu
    )
    override val selectedMessageBinding: LayoutSelectedMessageBinding by viewBinding(
        LayoutSelectedMessageBinding::bind, R.id.include_chat_selected_message
    )
    override val selectedMessageHolderBinding: LayoutMessageHolderBinding by viewBinding(
        LayoutMessageHolderBinding::bind, R.id.include_layout_message_holder_selected_message
    )

    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    override val viewModel: ChatTribeViewModel by viewModels()

    @Inject
    override lateinit var chatNavigator: TribeChatNavigator

    @Inject
    protected lateinit var imageLoaderInj: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.loadTribeAndPodcastData()?.let { podcast ->
                configurePodcastPlayer(podcast)
                addPodcastOnClickListeners(podcast)
            }
        }

        setupHeader()
    }

    private fun setupHeader() {
        val insetterActivity = (requireActivity() as InsetterActivity)

        binding.layoutMotionChat.getConstraintSet(R.id.motion_scene_chat_menu_closed)?.let { constraintSet ->
            val height = constraintSet.getConstraint(R.id.include_chat_header).layout.mHeight
            constraintSet.constrainHeight(R.id.include_chat_header, height + insetterActivity.statusBarInsetHeight.top)
        }

        binding.layoutMotionChat.getConstraintSet(R.id.motion_scene_chat_menu_open)?.let { constraintSet ->
            val height = constraintSet.getConstraint(R.id.include_chat_header).layout.mHeight
            constraintSet.constrainHeight(R.id.include_chat_header, height + insetterActivity.statusBarInsetHeight.top)
        }
    }

    override fun goToPaymentSendScreen() {}

    private fun configurePodcastPlayer(podcast: Podcast) {
        podcastPlayerBinding.apply {
            if (root.isGone) {
                scrollToBottom(callback = {
                    root.visible
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
                    podcast.getPlayingProgress()
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

    override suspend fun onViewStateFlowCollect(viewState: ActionsMenuViewState) {
        @Exhaustive
        when (viewState) {
            ActionsMenuViewState.Closed -> {
                binding.layoutMotionChat.setTransitionDuration(150)
            }
            ActionsMenuViewState.Open -> {
                binding.layoutMotionChat.setTransitionDuration(300)
            }
        }
        viewState.transitionToEndSet(binding.layoutMotionChat)
    }

    override fun getMotionLayouts(): Array<MotionLayout> {
        return arrayOf(binding.layoutMotionChat)
    }

    override fun onViewCreatedRestoreMotionScene(
        viewState: ActionsMenuViewState,
        binding: FragmentChatTribeBinding
    ) {
        viewState.restoreMotionScene(binding.layoutMotionChat)
    }
}
