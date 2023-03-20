package chat.sphinx.notification_level.ui

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.notification_level.R
import chat.sphinx.notification_level.databinding.FragmentNotificationLevelBinding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import chat.sphinx.wrapper_chat.NotificationLevel
import chat.sphinx.wrapper_chat.isMuteChat
import chat.sphinx.wrapper_chat.isOnlyMentions
import chat.sphinx.wrapper_chat.isSeeAll
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive

@AndroidEntryPoint
internal class NotificationLevelFragment: SideEffectDetailFragment<
        Context,
        NotificationLevelSideEffect,
        NotificationLevelViewState,
        NotificationLevelViewModel,
        FragmentNotificationLevelBinding
        >(R.layout.fragment_notification_level)
{
    override val viewModel: NotificationLevelViewModel by viewModels()
    override val binding: FragmentNotificationLevelBinding by viewBinding(FragmentNotificationLevelBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeNotificationLevelHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.notification_level_header_name)

            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        setupFragmentLayout()
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    private fun setupFragmentLayout() {
        binding.apply {
            (requireActivity() as InsetterActivity).addNavigationBarPadding(
                notificationLevelOptionsContainer
            )

            notificationLevelSeeAllContainer.setOnClickListener {
                viewModel.selectNotificationLevel(NotificationLevel.SeeAll)
            }

            notificationLevelOnlyMentionsContainer.setOnClickListener {
                viewModel.selectNotificationLevel(NotificationLevel.OnlyMentions)
            }

            notificationLevelMuteChatContainer.setOnClickListener {
                viewModel.selectNotificationLevel(NotificationLevel.MuteChat)
            }
        }
    }

    @SuppressLint("ResourceType")
    override suspend fun onViewStateFlowCollect(viewState: NotificationLevelViewState) {
        binding.apply {

            val selectedColor = root.context.getColor(R.color.primaryBlue)
            val unselectedColor = root.context.getColor(android.R.color.transparent)

            @Exhaustive
            when(viewState) {
                is NotificationLevelViewState.ChatNotificationLevel -> {
                    binding.notificationLevelSeeAllContainer.setBackgroundColor(
                        if (viewState.level == null || viewState.level.isSeeAll()) selectedColor else unselectedColor
                    )

                    binding.notificationLevelOnlyMentionsContainer.setBackgroundColor(
                        if (viewState.level?.isOnlyMentions() == true) selectedColor else unselectedColor
                    )

                    binding.notificationLevelMuteChatContainer.setBackgroundColor(
                        if (viewState.level?.isMuteChat() == true) selectedColor else unselectedColor
                    )
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: NotificationLevelSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
