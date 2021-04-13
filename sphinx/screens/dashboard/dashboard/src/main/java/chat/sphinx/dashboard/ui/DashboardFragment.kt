package chat.sphinx.dashboard.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentDashboardBinding
import chat.sphinx.dashboard.ui.adapter.ChatListAdapter
import chat.sphinx.dashboard.ui.adapter.ChatListFooterAdapter
import chat.sphinx.dashboard.ui.viewstates.NavDrawerViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.resources.inputMethodManager
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_screens.util.invisibleIfFalse
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_views.sideeffect.SideEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("NOTHING_TO_INLINE")
private inline fun FragmentDashboardBinding.searchBarClearFocus() {
    layoutDashboardSearchBar.editTextDashboardSearch.clearFocus()
}

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
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: DashboardViewModel by viewModels()
    override val binding: FragmentDashboardBinding by viewBinding(FragmentDashboardBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BackPressHandler(binding.root.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        viewModel.networkRefresh()

//        findNavController().addOnDestinationChangedListener(CloseDrawerOnDestinationChange())

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
                binding.searchBarClearFocus()
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
        val chatListAdapter = ChatListAdapter(imageLoader, viewLifecycleOwner, viewModel)
        val chatListFooterAdapter = ChatListFooterAdapter(viewLifecycleOwner, viewModel)
        binding.layoutDashboardChats.recyclerViewChats.apply {
            this.setHasFixedSize(false)
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = ConcatAdapter(chatListAdapter, chatListFooterAdapter)
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
                binding.searchBarClearFocus()
                lifecycleScope.launch { viewModel.navBarNavigator.toPaymentReceiveDetail() }
            }
            navBar.navBarButtonTransactions.setOnClickListener {
                binding.searchBarClearFocus()
                lifecycleScope.launch { viewModel.navBarNavigator.toTransactionsDetail() }
            }
            navBar.navBarButtonScanner.setOnClickListener {
                binding.searchBarClearFocus()
                lifecycleScope.launch { viewModel.navBarNavigator.toScannerDetail() }
            }
            navBar.navBarButtonPaymentSend.setOnClickListener {
                binding.searchBarClearFocus()
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

    override fun onPause() {
        super.onPause()
        binding.searchBarClearFocus()
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()
        lifecycleScope.launchWhenStarted {
            viewModel.networkStateFlow.collect { loadResponse ->
                binding.layoutDashboardHeader.let { dashboardHeader ->
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                            dashboardHeader.progressBarDashboardHeaderNetwork.invisibleIfFalse(true)
                            dashboardHeader.imageViewDashboardHeaderNetwork.invisibleIfFalse(false)
                        }
                        is Response.Error -> {
                            dashboardHeader.progressBarDashboardHeaderNetwork.invisibleIfFalse(false)
                            dashboardHeader.imageViewDashboardHeaderNetwork.invisibleIfFalse(true)
                            dashboardHeader.imageViewDashboardHeaderNetwork.setImageDrawable(
                                ContextCompat.getDrawable(
                                    binding.root.context,
                                    R.drawable.ic_network_state_white
                                ).also { drawable ->
                                    drawable?.setTint(
                                        ContextCompat.getColor(
                                            binding.root.context,
                                            R.color.primaryRed
                                        )
                                    )
                                }
                            )
                        }
                        is Response.Success -> {
                            dashboardHeader.progressBarDashboardHeaderNetwork.invisibleIfFalse(false)
                            dashboardHeader.imageViewDashboardHeaderNetwork.invisibleIfFalse(true)
                            dashboardHeader.imageViewDashboardHeaderNetwork.setImageDrawable(
                                ContextCompat.getDrawable(
                                    binding.root.context,
                                    R.drawable.ic_network_state_white
                                ).also { drawable ->
                                    drawable?.setTint(
                                        ContextCompat.getColor(
                                            binding.root.context,
                                            R.color.primaryGreen
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
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
                binding.layoutDashboardSearchBar.editTextDashboardSearch.let { editText ->
                    binding.root.context.inputMethodManager?.let { imm ->
                        if (imm.isActive(editText)) {
                            imm.hideSoftInputFromWindow(editText.windowToken, 0)
                            delay(250L)
                        }
                    }
                    binding.searchBarClearFocus()
                }
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
