package chat.sphinx.dashboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import chat.sphinx.dashboard.databinding.LayoutDashboardChatsFooterBinding
import chat.sphinx.dashboard.ui.ChatListViewModel
import chat.sphinx.dashboard.ui.viewstates.CreateTribeButtonViewState
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch

internal class ChatListFooterAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: ChatListViewModel
): RecyclerView.Adapter<ChatListFooterAdapter.FooterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = LayoutDashboardChatsFooterBinding.inflate(
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
        private val binding: LayoutDashboardChatsFooterBinding
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

            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                viewModel.createTribeButtonViewStateContainer.collect { viewState ->
                    @Exhaustive
                    when (viewState) {
                        is CreateTribeButtonViewState.Visible -> {
                            binding.layoutButtonCreateTribe.layoutConstraintButtonCreateTribe.visible
                        }
                        is CreateTribeButtonViewState.Hidden -> {
                            binding.layoutButtonCreateTribe.layoutConstraintButtonCreateTribe.gone
                        }
                    }
                }
            }
        }
    }
}
