package chat.sphinx.podcast_player.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.podcast_player.R
import chat.sphinx.podcast_player.databinding.FragmentTribeChatPodcastPlayerBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment


@AndroidEntryPoint
internal class TribeChatPodcastPlayerFragment : BaseFragment<
        TribeChatPodcastPlayerViewState,
        TribeChatPodcastPlayerViewModel,
        FragmentTribeChatPodcastPlayerBinding
        >(R.layout.fragment_tribe_chat_podcast_player) {
    override val viewModel: TribeChatPodcastPlayerViewModel by viewModels()
    override val binding: FragmentTribeChatPodcastPlayerBinding by viewBinding(FragmentTribeChatPodcastPlayerBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override suspend fun onViewStateFlowCollect(viewState: TribeChatPodcastPlayerViewState) {
//        TODO("Not yet implemented")
    }
}
