package chat.sphinx.leaderboard.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.leaderboard.R
import chat.sphinx.leaderboard.databinding.LayoutLeaderboardBinding
import chat.sphinx.leaderboard.ui.adapter.LeaderboardListAdapter
import chat.sphinx.leaderboard.ui.viewstate.LeaderboardViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import javax.inject.Inject

@AndroidEntryPoint
internal class LeaderboardFragment() : SideEffectFragment<
        FragmentActivity,
        LeaderboardSideEffect,
        LeaderboardViewState,
        LeaderboardViewModel,
        LayoutLeaderboardBinding
        >(R.layout.layout_leaderboard) {

    override val viewModel: LeaderboardViewModel by viewModels()
    override val binding: LayoutLeaderboardBinding by viewBinding(LayoutLeaderboardBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLeaderboardUsers()
    }

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override suspend fun onViewStateFlowCollect(viewState: LeaderboardViewState) {
    }

    override suspend fun onSideEffectCollect(sideEffect: LeaderboardSideEffect) {
    }


    private fun setupLeaderboardUsers(){
        binding.includeLayoutLeaderboardList.recyclerViewLeaderboardUserList.apply {
            val linearLayoutManager = LinearLayoutManager(context)
            val leaderboardListAdapter = LeaderboardListAdapter(
                this,
                linearLayoutManager,
                imageLoader,
                viewLifecycleOwner,
                viewModel
            )
        }
    }
}

