package chat.sphinx.tribe_badge.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.tribe_badge.databinding.LayoutBadgesListItemHolderBinding
import chat.sphinx.tribe_badge.ui.TribeBadgesViewModel

internal class TribeBadgesListAdapter(
    private val recyclerView: RecyclerView,
    private val layoutManager: LinearLayoutManager,
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: TribeBadgesViewModel,
):
    RecyclerView.Adapter<TribeBadgesListAdapter.TribeBadgeViewHolder>(),
    DefaultLifecycleObserver
{
    inner class TribeBadgeViewHolder(
        private val binding: LayoutBadgesListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TribeBadgeViewHolder {
        val binding = LayoutBadgesListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TribeBadgeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TribeBadgeViewHolder, position: Int) {
    }

    override fun getItemCount(): Int {
        return 10
    }
}