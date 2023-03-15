package chat.sphinx.known_badges.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.resources.databinding.LayoutBadgeItemFooterBinding

internal class KnownBadgesListFooterAdapter(
    private val insetterActivity: InsetterActivity
): RecyclerView.Adapter<KnownBadgesListFooterAdapter.FooterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = LayoutBadgeItemFooterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return FooterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {}

    override fun getItemCount(): Int {
        return 1
    }

    inner class FooterViewHolder(
        private val binding: LayoutBadgeItemFooterBinding
    ): RecyclerView.ViewHolder(binding.root) {
        init {
            insetterActivity.addNavigationBarPadding(binding.layoutConstraintItemListFooter)
        }
    }
}