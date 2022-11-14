package chat.sphinx.leaderboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.leaderboard.databinding.LayoutLeaderboardListUserHolderBinding
import chat.sphinx.leaderboard.ui.LeaderboardViewModel
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor

internal class LeaderboardListAdapter(
    private val recyclerView: RecyclerView,
    private val layoutManager: LinearLayoutManager,
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: LeaderboardViewModel,
):
    RecyclerView.Adapter<LeaderboardListAdapter.LeaderboardUserViewHolder>(), DefaultLifecycleObserver
{
    inner class LeaderboardUserViewHolder(
        private val binding: LayoutLeaderboardListUserHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardUserViewHolder {
        val binding = LayoutLeaderboardListUserHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LeaderboardUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderboardUserViewHolder, position: Int) {
    }

    override fun getItemCount(): Int {
        return 10
    }
}
