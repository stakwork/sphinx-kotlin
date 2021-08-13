package chat.sphinx.tribe_members_list.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.tribe_members_list.databinding.LayoutTribeMembersListFooterBinding

internal class TribeMembersListFooterAdapter(
    private val insetterActivity: InsetterActivity
): RecyclerView.Adapter<TribeMembersListFooterAdapter.FooterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = LayoutTribeMembersListFooterBinding.inflate(
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
        binding: LayoutTribeMembersListFooterBinding
    ): RecyclerView.ViewHolder(binding.root) {
        init {
            insetterActivity.addNavigationBarPadding(binding.layoutConstraintTribeMembersListFooter)
        }
    }
}