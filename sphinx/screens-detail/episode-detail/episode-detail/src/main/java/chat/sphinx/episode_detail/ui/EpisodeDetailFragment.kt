package chat.sphinx.episode_detail.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.episode_detail.R
import chat.sphinx.episode_detail.databinding.FragmentEpisodeDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment


@AndroidEntryPoint
internal class EpisodeDetailFragment: BaseFragment<
        EpisodeDetailViewState,
        EpisodeDetailViewModel,
        FragmentEpisodeDetailBinding
        >(R.layout.fragment_episode_detail)
{
    override val viewModel: EpisodeDetailViewModel by viewModels()
    override val binding: FragmentEpisodeDetailBinding by viewBinding(FragmentEpisodeDetailBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override suspend fun onViewStateFlowCollect(viewState: EpisodeDetailViewState) {

    }


}
