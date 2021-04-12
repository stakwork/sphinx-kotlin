package chat.sphinx.dashboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.dashboard.databinding.LayoutChatFooterBinding
import chat.sphinx.dashboard.ui.DashboardViewModel
import kotlinx.coroutines.launch

internal class ChatListFooterAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: DashboardViewModel
): RecyclerView.Adapter<ChatListFooterAdapter.FooterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = LayoutChatFooterBinding.inflate(
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
        private val binding: LayoutChatFooterBinding
    ): RecyclerView.ViewHolder(binding.root) {

        init {
            binding.layoutButtonAddFriend.layoutConstraintButtonAddFriend.setOnClickListener {
                lifecycleOwner.lifecycleScope.launch {
                    viewModel.navDrawerNavigator.toAddFriendDetail()
                }
            }
            binding.layoutButtonCreateTribe.layoutConstraintButtonCreateTribe.setOnClickListener {
                lifecycleOwner.lifecycleScope.launch {
                    viewModel.navDrawerNavigator.toCreateTribeDetail()
                }
            }
        }
    }
}