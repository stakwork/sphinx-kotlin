package chat.sphinx.dashboard.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentDashboardBinding
import chat.sphinx.dashboard.ui.viewstates.NavDrawerViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_views.sideeffect.SideEffect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive

@AndroidEntryPoint
internal class DashboardFragment : MotionLayoutFragment<
        Any,
        Nothing,
        SideEffect<Nothing>,
        NavDrawerViewState,
        DashboardViewModel,
        FragmentDashboardBinding
        >(R.layout.fragment_dashboard)
{
    override val viewModel: DashboardViewModel by viewModels()
    override val binding: FragmentDashboardBinding by viewBinding(FragmentDashboardBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BackPressHandler(binding.root.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        findNavController().addOnDestinationChangedListener(CloseDrawerOnDestinationChange())

        setupChats()
        setupDashboardHeader()
        setupNavBar()
        setupNavDrawer()
    }

    private inner class BackPressHandler(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            if (viewModel.currentViewState is NavDrawerViewState.Open) {
                viewModel.updateViewState(NavDrawerViewState.Closed)
            } else {
                super.handleOnBackPressed()
            }
        }
    }

    private inner class CloseDrawerOnDestinationChange: NavController.OnDestinationChangedListener {
        override fun onDestinationChanged(
            controller: NavController,
            destination: NavDestination,
            arguments: Bundle?
        ) {
            controller.removeOnDestinationChangedListener(this)
            viewModel.updateViewState(NavDrawerViewState.Closed)
        }
    }

    private fun setupChats() {
        binding.layoutDashboardChats.let { chats ->
            chats.dashboardButtonChatContact.setOnClickListener {
                lifecycleScope.launch { viewModel.dashboardNavigator.toChatContact("") }
            }
            chats.dashboardButtonChatGroup.setOnClickListener {
                lifecycleScope.launch { viewModel.dashboardNavigator.toChatGroup("") }
            }
            chats.dashboardButtonChatTribe.setOnClickListener {
                lifecycleScope.launch { viewModel.dashboardNavigator.toChatTribe("") }
            }
        }
    }

    private fun setupDashboardHeader() {
        binding.layoutDashboardHeader.let { header ->

            (requireActivity() as InsetterActivity)
                .addStatusBarPadding(header.layoutConstraintDashboardHeader)

            header.imageViewNavDrawerMenu.setOnClickListener {
                viewModel.updateViewState(NavDrawerViewState.Open)
            }
        }
    }

    private fun setupNavBar() {
        binding.layoutDashboardNavBar.let { navBar ->

            (requireActivity() as InsetterActivity)
                .addNavigationBarPadding(navBar.layoutConstraintDashboardNavBar)

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
    }

    private fun setupNavDrawer() {
        binding.dashboardNavDrawerInputLock.setOnClickListener {
            viewModel.updateViewState(NavDrawerViewState.Closed)
        }

        binding.layoutDashboardNavDrawer.let { navDrawer ->
            (requireActivity() as InsetterActivity)
                .addStatusBarPadding(navDrawer.layoutConstraintDashboardNavDrawer)
            (requireActivity() as InsetterActivity)
                .addNavigationBarPadding(navDrawer.layoutConstraintDashboardNavDrawer)

            navDrawer.layoutConstraintDashboardNavDrawer.setOnClickListener { viewModel }

            navDrawer.navDrawerButtonAddSats.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toAddSatsScreen() }
            }
            navDrawer.navDrawerButtonContacts.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toAddressBookScreen() }
            }
            navDrawer.navDrawerButtonProfile.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toProfileScreen() }
            }
            navDrawer.layoutButtonAddFriend.layoutConstraintButtonAddFriend.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toAddFriendDetail() }
            }
            navDrawer.layoutButtonCreateTribe.layoutConstraintButtonCreateTribe.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toCreateTribeDetail() }
            }
            navDrawer.navDrawerButtonSupportTicket.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toSupportTicketDetail() }
            }
            // TODO: Implement Private chat mode
//            navDrawer.navDrawerButtonLogout.setOnClickListener {
//                lifecycleScope.launch { viewModel.navDrawerNavigator.logout() }
//            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: NavDrawerViewState) {
        @Exhaustive
        when (viewState) {
            NavDrawerViewState.Closed -> {
                binding.layoutMotionDashboard.setTransitionDuration(150)
            }
            NavDrawerViewState.Open -> {
                binding.layoutMotionDashboard.setTransitionDuration(300)
            }
        }
        viewState.transitionToEndSet(binding.layoutMotionDashboard)
    }

    override fun onViewCreatedRestoreMotionScene(
        viewState: NavDrawerViewState,
        binding: FragmentDashboardBinding
    ) {
        viewState.restoreMotionScene(binding.layoutMotionDashboard)
    }

    override fun getMotionLayouts(): Array<MotionLayout> {
        return arrayOf(binding.layoutMotionDashboard)
    }

    override suspend fun onSideEffectCollect(sideEffect: SideEffect<Nothing>) {}
    override fun subscribeToSideEffectSharedFlow() {}
}
