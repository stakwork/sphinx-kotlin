package chat.sphinx.example.delete_media_detail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.delete.media.detail.databinding.LayoutDeleteMediaDetailsFooterBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding

internal class DeleteMediaFooterAdapter(
    private val insetterActivity: InsetterActivity
): RecyclerView.Adapter<DeleteMediaFooterAdapter.FooterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = LayoutDeleteMediaDetailsFooterBinding.inflate(
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
        binding: LayoutDeleteMediaDetailsFooterBinding
    ): RecyclerView.ViewHolder(binding.root) {
        init {
            insetterActivity.addNavigationBarPadding(binding.layoutConstraintDeleteMediaDetailsFooter)
        }
    }
}