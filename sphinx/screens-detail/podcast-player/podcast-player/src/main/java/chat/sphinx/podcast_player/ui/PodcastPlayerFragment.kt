package chat.sphinx.podcast_player.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.podcast_player.R
import chat.sphinx.podcast_player.databinding.FragmentPodcastPlayerBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch


@AndroidEntryPoint
internal class PodcastPlayerFragment : BaseFragment<
        PodcastPlayerViewState,
        PodcastPlayerViewModel,
        FragmentPodcastPlayerBinding
        >(R.layout.fragment_podcast_player) {
    override val viewModel: PodcastPlayerViewModel by viewModels()
    override val binding: FragmentPodcastPlayerBinding by viewBinding(FragmentPodcastPlayerBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textViewDismissButton.setOnClickListener {
            lifecycleScope.launch(viewModel.mainImmediate) {
                viewModel.navigator.closeDetailScreen()
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: PodcastPlayerViewState) {
//        TODO("Not yet implemented")
    }
}
