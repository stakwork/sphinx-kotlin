package chat.sphinx.chat_tribe.ui

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.chat_common.databinding.LayoutChatFooterBinding
import chat.sphinx.chat_common.databinding.LayoutChatHeaderBinding
import chat.sphinx.chat_common.navigation.ChatNavigator
import chat.sphinx.chat_common.ui.ChatFragment
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.databinding.FragmentChatTribeBinding
import chat.sphinx.chat_tribe.databinding.LayoutPodcastPlayerFooterBinding
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_network_query_chat.model.PodcastDto
import chat.sphinx.podcast_player.objects.Podcast
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.goneIfFalse
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
    private val podcastPlayerBinding: LayoutPodcastPlayerFooterBinding by viewBinding(LayoutPodcastPlayerFooterBinding::bind, R.id.include_podcast_player_footer)

    override val binding: FragmentChatTribeBinding by viewBinding(FragmentChatTribeBinding::bind)

    override val footerBinding: LayoutChatFooterBinding by viewBinding(LayoutChatFooterBinding::bind, R.id.include_chat_tribe_footer)
    override val headerBinding: LayoutChatHeaderBinding by viewBinding(LayoutChatHeaderBinding::bind, R.id.include_chat_tribe_header)

    override val recyclerView: RecyclerView
        get() = binding.recyclerViewMessages

    override val viewModel: ChatTribeViewModel by viewModels()

    @Inject
    protected lateinit var imageLoaderInj: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    @Inject
    override lateinit var chatNavigator: TribeChatNavigator


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.loadTribeAndPodcastData().collect { podcast ->
                configurePodcastPlayer(podcast)
            }
        }
    }

    private fun configurePodcastPlayer(podcast: Podcast) {
        podcastPlayerBinding.apply {
            scrollToBottom(callback = {
                root.goneIfFalse(true)
            })

            val episode = podcast.getCurrentEpisode()
            textViewEpisodeTitle.text = episode.title

            textViewPlayPauseButton.setOnClickListener {
                //TODO: Start service and send action to Podcast Player Service
            }

            textViewEpisodeTitle.setOnClickListener {
                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    chatNavigator.toPodcastPlayerScreen(podcast)
                }
            }
        }
    }
}
