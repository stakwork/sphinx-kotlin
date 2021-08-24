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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentDashboardBinding
import chat.sphinx.dashboard.ui.adapter.ChatListAdapter
import chat.sphinx.dashboard.ui.adapter.ChatListFooterAdapter
import chat.sphinx.dashboard.ui.viewstates.DeepLinkPopupViewState
import chat.sphinx.dashboard.ui.viewstates.NavDrawerViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.invisibleIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_views.viewstate.collect
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
        Context,
        DashboardSideEffect,
        NavDrawerViewState,
        DashboardViewModel,
        FragmentDashboardBinding
        >(R.layout.fragment_dashboard)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var userColorsHelper: UserColorsHelper

    override val viewModel: DashboardViewModel by viewModels()
    override val binding: FragmentDashboardBinding by viewBinding(FragmentDashboardBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BackPressHandler(binding.root.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        viewModel.networkRefresh()

        findNavController().addOnDestinationChangedListener(CloseDrawerOnDestinationChange())

        setupChats()
        setupDashboardHeader()
        setupNavBar()
        setupNavDrawer()
        setupPopups()
    }

    override fun onResume() {
        super.onResume()

        activity?.intent?.dataString?.let { deepLink ->
            viewModel.handleDeepLink(deepLink)
            activity?.intent?.data = null
        }
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
        binding.layoutDashboardChats.recyclerViewChats.apply {
            val linearLayoutManager = LinearLayoutManager(context)
            val chatListAdapter = ChatListAdapter(
                this,
                linearLayoutManager,
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                userColorsHelper
            )

            val chatListFooterAdapter = ChatListFooterAdapter(viewLifecycleOwner, viewModel)
            this.setHasFixedSize(false)
            layoutManager = linearLayoutManager
            adapter = ConcatAdapter(chatListAdapter, chatListFooterAdapter)
            itemAnimator = null
        }
    }

    private fun setupDashboardHeader() {
        binding.layoutDashboardHeader.let { header ->
            val activity = (requireActivity() as InsetterActivity)

            activity.addStatusBarPadding(header.layoutConstraintDashboardHeader)

            val newHeaderHeight = header.layoutConstraintDashboardHeader.layoutParams.height + activity.statusBarInsetHeight.top

            binding.layoutMotionDashboard.getConstraintSet(R.id.motion_scene_dashboard_drawer_closed)?.let { constraintSet ->
                constraintSet.constrainHeight(R.id.layout_dashboard_header, newHeaderHeight)
            }

            binding.layoutMotionDashboard.getConstraintSet(R.id.motion_scene_dashboard_drawer_open)?.let { constraintSet ->
                constraintSet.constrainHeight(R.id.layout_dashboard_header, newHeaderHeight)
            }

            header.imageViewNavDrawerMenu.setOnClickListener {
                viewModel.updateViewState(NavDrawerViewState.Open)
            }

            header.textViewDashboardHeaderUpgradeApp.setOnClickListener {
                viewModel.goToAppUpgrade()
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
                viewModel.toScanner()
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

    private fun setupPopups() {
        binding.layoutDashboardPopup.layoutDashboardAuthorizePopup.apply {
            textViewDashboardPopupClose.setOnClickListener {
                viewModel.deepLinkPopupViewStateContainer.updateViewState(
                    DeepLinkPopupViewState.PopupDismissed
                )
            }

            buttonAuthorize.setOnClickListener {
                progressBarAuthorize.visible
                viewModel.authorizeExternal()
            }
        }

        binding.layoutDashboardPopup.layoutDashboardConnectPopup.apply {
            textViewDashboardPopupClose.setOnClickListener {
                viewModel.deepLinkPopupViewStateContainer.updateViewState(
                    DeepLinkPopupViewState.PopupDismissed
                )
            }

            buttonConnect.setOnClickListener {
                progressBarConnect.visible

                viewModel.connectToContact(
                    editTextDashboardPeoplePopupMessage.text.toString()
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            binding.layoutDashboardHeader.textViewDashboardHeaderUpgradeApp.goneIfFalse(
                viewModel.getNewVersionAvailable()
            )
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.getAccountBalance().collect { nodeBalance ->
                if (nodeBalance == null) return@collect

                nodeBalance.balance.asFormattedString().let { balance ->
                    binding.layoutDashboardHeader.textViewDashboardHeaderBalance.text = balance
                    binding.layoutDashboardNavDrawer.navDrawerTextViewSatsBalance.text = balance
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.networkStateFlow.collect { loadResponse ->
                binding.layoutDashboardHeader.let { dashboardHeader ->
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                            dashboardHeader.progressBarDashboardHeaderNetwork.invisibleIfFalse(true)
                            dashboardHeader.textViewDashboardHeaderNetwork.invisibleIfFalse(false)
                        }
                        is Response.Error -> {
                            dashboardHeader.progressBarDashboardHeaderNetwork.invisibleIfFalse(false)
                            dashboardHeader.textViewDashboardHeaderNetwork.invisibleIfFalse(true)
                            dashboardHeader.textViewDashboardHeaderNetwork.setTextColor(
                                ContextCompat.getColor(
                                    binding.root.context,
                                    R.color.primaryRed
                                )
                            )
                        }
                        is Response.Success -> {
                            dashboardHeader.progressBarDashboardHeaderNetwork.invisibleIfFalse(false)
                            dashboardHeader.textViewDashboardHeaderNetwork.invisibleIfFalse(true)
                            dashboardHeader.textViewDashboardHeaderNetwork.setTextColor(
                                ContextCompat.getColor(
                                    binding.root.context,
                                    R.color.primaryGreen
                                )
                            )
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.accountOwnerStateFlow.collect { contactOwner ->
                contactOwner?.let { owner ->
                    owner.photoUrl?.value?.let { url ->
                        imageLoader.load(
                            binding.layoutDashboardNavDrawer.navDrawerImageViewUserProfilePicture,
                            url,
                            ImageLoaderOptions.Builder()
                                .placeholderResId(R.drawable.ic_profile_avatar_circle)
                                .transformation(Transformation.CircleCrop)
                                .build()
                        )
                    } ?: binding.layoutDashboardNavDrawer
                        .navDrawerImageViewUserProfilePicture
                        .setImageDrawable(
                            ContextCompat.getDrawable(
                                binding.root.context,
                                R.drawable.ic_profile_avatar_circle
                            )
                        )
                    binding.layoutDashboardNavDrawer.navDrawerTextViewProfileName.text =
                        owner.alias?.value ?: ""
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.searchBarClearFocus()
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

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deepLinkPopupViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is DeepLinkPopupViewState.PopupDismissed -> {
                        binding.layoutDashboardPopup.apply {

                            layoutDashboardAuthorizePopup.apply {
                                root.gone
                                progressBarAuthorize.gone
                            }

                            layoutDashboardConnectPopup.apply {
                                root.gone
                                progressBarConnect.gone
                            }

                            root.gone
                        }
                    }
                    is DeepLinkPopupViewState.ExternalAuthorizePopup -> {
                        binding.layoutDashboardPopup.apply {

                            layoutDashboardAuthorizePopup.apply {
                                textViewDashboardPopupAuthorizeName.text = viewState.host
                                layoutConstraintAuthorizePopup.visible
                                root.visible
                            }

                            root.visible
                        }
                    }
                    is DeepLinkPopupViewState.LoadingPeopleConnectPopup -> {
                        binding.layoutDashboardPopup.apply {

                            layoutDashboardConnectPopup.apply {
                                layoutConstraintDashboardConnectLoading.visible
                                root.visible
                            }

                            root.visible
                        }
                    }
                    is DeepLinkPopupViewState.PeopleConnectPopup -> {
                        binding.layoutDashboardPopup.apply {

                            layoutDashboardConnectPopup.apply {
                                val alias = viewState.alias ?: getString(R.string.unknown)
                                textViewDashboardPeoplePopupName.text = alias

                                editTextDashboardPeoplePopupMessage.hint = getString(R.string.dashboard_connect_initial_message_hint, alias)
                                textViewDashboardPeoplePopupDescription.text = viewState.description ?: "No Description"

                                val priceToMeet = (viewState.priceToMeet ?: 0).toSat()?.asFormattedString(appendUnit = true) ?: ""
                                textViewDashboardPeoplePopupPriceToMeet.text = getString(R.string.dashboard_connect_price_to_meet, priceToMeet)

                                viewState.photoUrl?.let { url ->
                                    imageLoader.load(
                                        imageViewProfilePicture,
                                        url,
                                        ImageLoaderOptions.Builder()
                                            .placeholderResId(R.drawable.ic_profile_avatar_circle)
                                            .transformation(Transformation.CircleCrop)
                                            .build()
                                    )
                                } ?: imageViewProfilePicture.setImageDrawable(
                                        ContextCompat.getDrawable(
                                            binding.root.context,
                                            R.drawable.ic_profile_avatar_circle
                                        )
                                    )

                                layoutConstraintDashboardConnectLoading.gone
                                root.visible
                            }

                            root.visible
                        }
                    }
                }
            }
        }
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

    override suspend fun onSideEffectCollect(sideEffect: DashboardSideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
