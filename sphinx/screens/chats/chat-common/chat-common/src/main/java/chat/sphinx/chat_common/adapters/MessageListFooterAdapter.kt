package chat.sphinx.chat_common.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.chat_common.databinding.LayoutMessageListFooterBinding

/**
 * Needed in order to have the last item of the RecyclerView
 * be able to scroll up over the device's navigation bar.
 * */
internal class MessageListFooterAdapter: RecyclerView.Adapter<MessageListFooterAdapter.FooterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = LayoutMessageListFooterBinding.inflate(
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
        binding: LayoutMessageListFooterBinding
    ): RecyclerView.ViewHolder(binding.root)
}
