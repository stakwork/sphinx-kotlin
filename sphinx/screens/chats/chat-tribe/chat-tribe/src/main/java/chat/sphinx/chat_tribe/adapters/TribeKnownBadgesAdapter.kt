package chat.sphinx.chat_tribe.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.chat_tribe.ui.BadgesListViewModel
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.resources.databinding.LayoutKnownBadgesListItemHolderBinding

internal class TribeKnownBadgesAdapter(
    private val recyclerView: RecyclerView,
    private val layoutManager: LinearLayoutManager,
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: BadgesListViewModel,
):
    RecyclerView.Adapter<TribeKnownBadgesAdapter.TribeknownBadgeViewHolder>(),
    DefaultLifecycleObserver
{
    inner class TribeknownBadgeViewHolder(
        private val binding: LayoutKnownBadgesListItemHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TribeknownBadgeViewHolder {
        val binding = LayoutKnownBadgesListItemHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TribeknownBadgeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TribeknownBadgeViewHolder, position: Int) {
    }

    override fun getItemCount(): Int {
        return 10
    }
}