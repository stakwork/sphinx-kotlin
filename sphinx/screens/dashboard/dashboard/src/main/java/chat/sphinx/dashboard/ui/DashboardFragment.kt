package chat.sphinx.dashboard.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentDashboardBinding
import chat.sphinx.dashboard.ui.viewstates.DashboardChatViewState
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class DashboardFragment: BaseFragment<
        DashboardChatViewState,
        DashboardViewModel,
        FragmentDashboardBinding
        >(R.layout.fragment_dashboard)
{
    override val viewModel: DashboardViewModel by viewModels()
    override val binding: FragmentDashboardBinding by viewBinding(FragmentDashboardBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CloseAppOnBackPress(view.context)
            .enableDoubleTapToClose(
                viewLifecycleOwner,
                SphinxToastUtils()
            )
            .addCallback(
                viewLifecycleOwner,
                requireActivity()
            )
        binding.dashboardButtonChatContact.setOnClickListener {
            lifecycleScope.launch { viewModel.dashboardNavigator.toChatContact("") }
        }
        binding.dashboardButtonChatGroup.setOnClickListener {
            lifecycleScope.launch { viewModel.dashboardNavigator.toChatGroup("") }
        }
        binding.dashboardButtonChatTribe.setOnClickListener {
            lifecycleScope.launch { viewModel.dashboardNavigator.toChatTribe("") }
        }
        binding.layoutNavBar.let { navBar ->
            navBar.navBarButtonPaymentReceive.setOnClickListener {
                lifecycleScope.launch { viewModel.navBarNavigator.toPaymentReceiveDetail() }
            }
            navBar.navBarButtonTransactions.setOnClickListener {
                lifecycleScope.launch { viewModel.navBarNavigator.toTransactionsDetail() }
            }
            navBar.navBarButtonScanner.setOnClickListener {
                lifecycleScope.launch { viewModel.navBarNavigator.toScannerDetail() }
            }
            navBar.navBarButtonPaymentSend.setOnClickListener {
                lifecycleScope.launch { viewModel.navBarNavigator.toPaymentSendDetail() }
            }
        }
        binding.layoutNavDrawer.let { navDrawer ->
            navDrawer.navDrawerButtonAddSats.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toAddSatsScreen() }
            }
            navDrawer.navDrawerButtonAddressBook.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toAddressBookScreen() }
            }
            navDrawer.navDrawerButtonProfile.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toProfileScreen() }
            }
            navDrawer.navDrawerButtonAddFriend.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toAddFriendDetail() }
            }
            navDrawer.navDrawerButtonCreateTribe.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toCreateTribeDetail() }
            }
            navDrawer.navDrawerButtonSupportTicket.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toSupportTicketDetail() }
            }
            navDrawer.navDrawerButtonLogout.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.logout() }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: DashboardChatViewState) {
//        TODO("Not yet implemented")
    }
}
