package chat.sphinx.dashboard.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentDashboardBinding
import chat.sphinx.dashboard.ui.viewstates.*
import chat.sphinx.dashboard.ui.viewstates.DashboardMotionViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.resources.databinding.LayoutPodcastPlayerFooterBinding
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_view.Px
import chat.sphinx.swipe_reveal_layout.SwipeRevealLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_screens.util.*
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class DashboardFragment : MotionLayoutFragment<
        Any,
        Context,
        ChatListSideEffect,
        DashboardMotionViewState,
        DashboardViewModel,
        FragmentDashboardBinding
        >(R.layout.fragment_dashboard), SwipeRefreshLayout.OnRefreshListener
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: DashboardViewModel by viewModels()
    private val dashboardPodcastViewModel: DashboardPodcastViewModel by viewModels()

    override val binding: FragmentDashboardBinding by viewBinding(FragmentDashboardBinding::bind)

    private val podcastPlayerBinding: LayoutPodcastPlayerFooterBinding
        get() = binding.layoutPodcastPlayerFooter

    var timeTrackerStart: Long = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.screenInit()

        findNavController().addOnDestinationChangedListener(CloseDrawerOnDestinationChange())

        setupViewPager()
        setupDashboardHeader()
        setupFooter()
        setupNavDrawer()
        setupPopups()
        setupRestorePopup()
    }

    override fun onPause() {
        super.onPause()

        dashboardPodcastViewModel.trackPodcastConsumed()
    }

    override fun onResume() {
        super.onResume()

        viewModel.networkRefresh()

        activity?.intent?.dataString?.let { deepLink ->
            viewModel.handleDeepLink(deepLink)
            activity?.intent?.data = null
        }
    }

    override fun onRefresh() {
        binding.swipeRefreshLayoutDataReload.isRefreshing = false
        viewModel.networkRefresh()
    }

    fun closeDrawerIfOpen(): Boolean {
        if (viewModel.currentViewState is DashboardMotionViewState.DrawerOpenNavBarHidden) {
            viewModel.updateViewState(DashboardMotionViewState.DrawerCloseNavBarHidden)
            return true
        } else if (viewModel.currentViewState is DashboardMotionViewState.DrawerOpenNavBarVisible) {
            viewModel.updateViewState(DashboardMotionViewState.DrawerCloseNavBarVisible)
            return true
        }
        return false
    }

    private inner class CloseDrawerOnDestinationChange: NavController.OnDestinationChangedListener {
        override fun onDestinationChanged(
            controller: NavController,
            destination: NavDestination,
            arguments: Bundle?
        ) {
            controller.removeOnDestinationChangedListener(this)
            viewModel.updateViewState(DashboardMotionViewState.DrawerCloseNavBarVisible)
        }
    }

    private fun setupViewPager() {
        binding.apply {
            swipeRefreshLayoutDataReload.setOnRefreshListener(this@DashboardFragment)

            val dashboardFragmentsAdapter = DashboardFragmentsAdapter(
                this@DashboardFragment
            )

            viewPagerDashboardTabs.adapter = dashboardFragmentsAdapter
            viewPagerDashboardTabs.isUserInputEnabled = false

            val tabs = tabLayoutDashboardTabs

            TabLayoutMediator(tabs, viewPagerDashboardTabs) { tab, position ->
                tab.text = dashboardFragmentsAdapter.getPageTitle(position)
            }.attach()

            viewPagerDashboardTabs.offscreenPageLimit = 3
            
            viewPagerDashboardTabs.post {
                viewPagerDashboardTabs.currentItem = viewModel.getCurrentPagePosition()

                viewPagerDashboardTabs.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                    override fun onPageScrolled(
                        position: Int,
                        positionOffset: Float,
                        positionOffsetPixels: Int
                    ) { }

                    override fun onPageSelected(position: Int) {
                        viewModel.updateTabsState(
                            feedActive = position == DashboardFragmentsAdapter.FEED_TAB_POSITION,
                            friendsActive = position == DashboardFragmentsAdapter.FRIENDS_TAB_POSITION,
                            tribesActive = position == DashboardFragmentsAdapter.TRIBES_TAB_POSITION,
                        )
                    }

                    override fun onPageScrollStateChanged(state: Int) { }
                })
            }

            val feedTab: View = LayoutInflater.from(this@DashboardFragment.context)
                .inflate(R.layout.layout_dashboard_custom_tab, tabs, false)
            tabs.getTabAt(DashboardFragmentsAdapter.FEED_TAB_POSITION)?.customView = feedTab

            val feedTitle = DashboardFragmentsAdapter.TAB_TITLES[DashboardFragmentsAdapter.FEED_TAB_POSITION]
            feedTab?.findViewById<TextView>(R.id.text_view_tab_title)?.text = getString(feedTitle)

            val friendsTab: View = LayoutInflater.from(this@DashboardFragment.context)
                .inflate(R.layout.layout_dashboard_custom_tab, tabs, false)
            tabs.getTabAt(DashboardFragmentsAdapter.FRIENDS_TAB_POSITION)?.customView = friendsTab

            val friendsTitle = DashboardFragmentsAdapter.TAB_TITLES[DashboardFragmentsAdapter.FRIENDS_TAB_POSITION]
            friendsTab?.findViewById<TextView>(R.id.text_view_tab_title)?.text = getString(friendsTitle)

            val tribesTab: View = LayoutInflater.from(this@DashboardFragment.context)
                .inflate(R.layout.layout_dashboard_custom_tab, tabs, false)
            tabs.getTabAt(DashboardFragmentsAdapter.TRIBES_TAB_POSITION)?.customView = tribesTab

            val tribesTitle = DashboardFragmentsAdapter.TAB_TITLES[DashboardFragmentsAdapter.TRIBES_TAB_POSITION]
            tribesTab?.findViewById<TextView>(R.id.text_view_tab_title)?.text = getString(tribesTitle)
        }
    }

    private fun setupDashboardHeader() {
        binding.layoutDashboardHeader.let { header ->
            val activity = (requireActivity() as InsetterActivity)

            activity.addStatusBarPadding(header.layoutConstraintDashboardHeader)

            val newHeaderHeight = header.layoutConstraintDashboardHeader.layoutParams.height + activity.statusBarInsetHeight.top

            binding.layoutMotionDashboard.getConstraintSet(R.id.motion_scene_dashboard_default)?.let { constraintSet ->
                constraintSet.constrainHeight(R.id.layout_dashboard_header, newHeaderHeight)
            }

            binding.layoutMotionDashboard.getConstraintSet(R.id.motion_scene_dashboard_drawer_open_nav_bar_visible)?.let { constraintSet ->
                constraintSet.constrainHeight(R.id.layout_dashboard_header, newHeaderHeight)
            }

            binding.layoutMotionDashboard.getConstraintSet(R.id.motion_scene_dashboard_drawer_open_nav_bar_hidden)?.let { constraintSet ->
                constraintSet.constrainHeight(R.id.layout_dashboard_header, newHeaderHeight)
            }

            binding.layoutMotionDashboard.getConstraintSet(R.id.motion_scene_dashboard_nav_bar_hidden)?.let { constraintSet ->
                constraintSet.constrainHeight(R.id.layout_dashboard_header, newHeaderHeight)
            }

            header.imageViewNavDrawerMenu.setOnClickListener {
                if (viewModel.currentViewState is DashboardMotionViewState.DrawerCloseNavBarVisible) {
                    viewModel.updateViewState(DashboardMotionViewState.DrawerOpenNavBarVisible)
                } else if (viewModel.currentViewState is DashboardMotionViewState.DrawerCloseNavBarHidden) {
                    viewModel.updateViewState(DashboardMotionViewState.DrawerOpenNavBarHidden)
                }
            }

            header.textViewDashboardHeaderUpgradeApp.setOnClickListener {
                viewModel.goToAppUpgrade()
            }
            header.textViewDashboardHeaderNetwork.setOnClickListener {
                viewModel.toastIfNetworkConnected()
            }
        }
    }

    private fun setupFooter() {
        (requireActivity() as InsetterActivity).addNavigationBarPadding(binding.root)

        setupNavBar()
        setupPodcastPlayerFooter()
    }

    private fun setupNavBar() {
        binding.layoutDashboardNavBar.let { navBar ->

            navBar.navBarButtonPaymentReceive.setOnClickListener {
                lifecycleScope.launch { viewModel.navBarNavigator.toPaymentReceiveDetail() }
            }
            navBar.navBarButtonTransactions.setOnClickListener {
                lifecycleScope.launch { viewModel.navBarNavigator.toTransactionsDetail() }
            }
            navBar.navBarButtonScanner.setOnClickListener {
                viewModel.toScanner()
            }
            navBar.navBarButtonPaymentSend.setOnClickListener {
                lifecycleScope.launch { viewModel.navBarNavigator.toPaymentSendDetail() }
            }
        }
    }

    private fun onPodcastBarDismissed() {
        dashboardPodcastViewModel.forcePodcastStop()
        dashboardPodcastViewModel.trackPodcastConsumed()

        podcastPlayerBinding.root.gone
        binding.swipeRevealLayoutPlayer.gone
        binding.imageViewBottomBarShadow.visible
    }

    private fun setupPodcastPlayerFooter() {
        binding.swipeRevealLayoutPlayer.setSwipeListener(object: SwipeRevealLayout.SwipeListener {
            override fun onClosed(view: SwipeRevealLayout?) {}

            override fun onOpened(view: SwipeRevealLayout?) {
                onPodcastBarDismissed()
            }

            override fun onSlide(view: SwipeRevealLayout?, slideOffset: Float) {}
        })

        podcastPlayerBinding.apply {
            imageViewForward30Button.setOnClickListener {
                dashboardPodcastViewModel.playingPodcastViewStateContainer.value.clickFastForward?.invoke()
            }
            textViewPlayButton.setOnClickListener {
                dashboardPodcastViewModel.playingPodcastViewStateContainer.value.clickPlayPause?.invoke()
            }
            animationViewPauseButton.setOnClickListener {
                dashboardPodcastViewModel.playingPodcastViewStateContainer.value.clickPlayPause?.invoke()
            }
            layoutConstraintPodcastInfo.setOnClickListener {
                dashboardPodcastViewModel.playingPodcastViewStateContainer.value.clickTitle?.invoke()
            }
        }
    }

    private fun setupRestorePopup() {
        binding.layoutDashboardRestore.layoutDashboardRestoreProgress.apply {
            buttonStopRestore.setOnClickListener {
                viewModel.cancelRestore()
            }
        }
    }

    private fun setupNavDrawer() {
        binding.dashboardNavDrawerInputLock.setOnClickListener {
            if (viewModel.currentViewState is DashboardMotionViewState.DrawerOpenNavBarVisible) {
                viewModel.updateViewState(DashboardMotionViewState.DrawerCloseNavBarVisible)
            } else if (viewModel.currentViewState is DashboardMotionViewState.DrawerOpenNavBarHidden) {
                viewModel.updateViewState(DashboardMotionViewState.DrawerCloseNavBarHidden)
            }
        }

        binding.layoutDashboardNavDrawer.let { navDrawer ->
            (requireActivity() as InsetterActivity)
                .addStatusBarPadding(navDrawer.layoutConstraintDashboardNavDrawer)
                .addNavigationBarPadding(navDrawer.layoutConstraintDashboardNavDrawer)

            navDrawer.layoutConstraintDashboardNavDrawer.setOnClickListener { viewModel }

            navDrawer.navDrawerButtonContacts.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toAddressBookScreen() }
            }
            navDrawer.navDrawerButtonProfile.setOnClickListener {
                lifecycleScope.launch { viewModel.navDrawerNavigator.toProfileScreen() }
            }
            navDrawer.navDrawerButtonHeaderProfile.setOnClickListener {
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
                viewModel.authorizeExternal()
            }
        }

        binding.layoutDashboardPopup.layoutDashboardPeopleProfilePopup.apply {
            textViewDashboardPopupClose.setOnClickListener {
                viewModel.deepLinkPopupViewStateContainer.updateViewState(
                    DeepLinkPopupViewState.PopupDismissed
                )
            }

            buttonSaveProfile.setOnClickListener {
                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    viewModel.updatePeopleProfile()
                }
            }
        }

        binding.layoutDashboardPopup.layoutDashboardConnectPopup.apply {
            textViewDashboardPopupClose.setOnClickListener {
                viewModel.deepLinkPopupViewStateContainer.updateViewState(
                    DeepLinkPopupViewState.PopupDismissed
                )
            }

            buttonConnect.setOnClickListener {
                viewModel.connectToContact(
                    editTextDashboardPeoplePopupMessage.text?.toString()
                )
            }
        }
    }

    fun shouldToggleNavBar(show: Boolean) {
        if (show) {
            viewModel.updateViewState(DashboardMotionViewState.DrawerCloseNavBarVisible)
        } else {
            viewModel.updateViewState(DashboardMotionViewState.DrawerCloseNavBarHidden)
        }
    }

    override fun onStart() {
        super.onStart()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.newVersionAvailable.asStateFlow().collect { newVersionAvailable ->
                binding.layoutDashboardHeader.textViewDashboardHeaderUpgradeApp.goneIfFalse(
                    newVersionAvailable
                )
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.currentVersion.asStateFlow().collect { currentVersion ->
                binding.layoutDashboardNavDrawer.textViewNavDrawerVersionNumber.apply {
                    text = currentVersion
                }
            }
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
                            timeTrackerStart = System.currentTimeMillis()
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
                            Log.d("TimeTracker", "Your node went online in ${System.currentTimeMillis() - timeTrackerStart} milliseconds")
                            viewModel.sendAppLog("- Your node went online in ${System.currentTimeMillis() - timeTrackerStart} milliseconds")
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.restoreStateFlow.collect { response ->
                binding.layoutDashboardRestore.apply {
                    if (response != null) {
                        layoutDashboardRestoreProgress.apply {
                            val progressString = "${response.progress}%"

                            textViewRestoreProgress.text = getString(R.string.dashboard_restore_progress, progressString)
                            progressBarRestore.progress = response.progress
                        }
                        root.visible
                    } else {
                        root.gone
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

    override suspend fun onViewStateFlowCollect(viewState: DashboardMotionViewState) {
        @Exhaustive
        when (viewState) {
            DashboardMotionViewState.DrawerCloseNavBarHidden -> {
                binding.layoutMotionDashboard.setTransitionDuration(150)
            }
            DashboardMotionViewState.DrawerCloseNavBarVisible -> {
                binding.layoutMotionDashboard.setTransitionDuration(150)
            }
            DashboardMotionViewState.DrawerOpenNavBarHidden -> {
                binding.layoutMotionDashboard.setTransitionDuration(300)
            }
            DashboardMotionViewState.DrawerOpenNavBarVisible -> {
                binding.layoutMotionDashboard.setTransitionDuration(300)
            }
        }
        viewState.transitionToEndSet(binding.layoutMotionDashboard)
    }

    private val progressWidth: Px by lazy {
        Px(binding.root.measuredWidth.toFloat())
    }

    private var disposable: Disposable? = null
    private var imageJob: Job? = null

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.tabsViewStateContainer.collect { viewState ->
                when (viewState) {
                    is DashboardTabsViewState.Idle -> {}

                    is DashboardTabsViewState.TabsState -> {
                        val tabs = binding.tabLayoutDashboardTabs

                        val feedTab = tabs.getTabAt(DashboardFragmentsAdapter.FEED_TAB_POSITION)?.customView
                        val friendsTab = tabs.getTabAt(DashboardFragmentsAdapter.FRIENDS_TAB_POSITION)?.customView
                        val tribesTab = tabs.getTabAt(DashboardFragmentsAdapter.TRIBES_TAB_POSITION)?.customView

                        feedTab?.findViewById<TextView>(R.id.text_view_tab_title)?.setTextColor(
                            ContextCompat.getColor(
                                binding.root.context,
                                if (viewState.feedActive) R.color.text else R.color.secondaryText
                            )
                        )

                        friendsTab?.findViewById<TextView>(R.id.text_view_tab_title)?.setTextColor(
                            ContextCompat.getColor(
                                binding.root.context,
                                if (viewState.friendsActive) R.color.text else R.color.secondaryText
                            )
                        )

                        friendsTab?.findViewById<View>(R.id.view_unseen_messages_dot)?.goneIfFalse(
                            viewState.friendsBadgeVisible
                        )

                        tribesTab?.findViewById<TextView>(R.id.text_view_tab_title)?.setTextColor(
                            ContextCompat.getColor(
                                binding.root.context,
                                if (viewState.tribesActive) R.color.text else R.color.secondaryText
                            )
                        )

                        tribesTab?.findViewById<View>(R.id.view_unseen_messages_dot)?.goneIfFalse(
                            viewState.tribesBadgeVisible
                        )
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            dashboardPodcastViewModel.playingPodcastViewStateContainer.collect { viewState ->
                podcastPlayerBinding.apply {
                    when (viewState) {
                        is PlayingPodcastViewState.NoPodcast -> {
                            root.gone

                            binding.apply {
                                swipeRevealLayoutPlayer.gone
                                imageViewPlayerBarShadow.gone
                                imageViewBottomBarShadow.visible
                            }
                        }
                        is PlayingPodcastViewState.PodcastVS -> {
                            textViewPlayButton.goneIfFalse(viewState.showPlayButton && !viewState.showLoading)
                            animationViewPauseButton.goneIfFalse(!viewState.showPlayButton && !viewState.showLoading)

                            progressBar.progress = viewState.playingProgress

                            podcastPlayerBinding.textViewEpisodeTitle.isSelected = !viewState.showPlayButton && !viewState.showLoading
                            textViewEpisodeTitle.text = viewState.title
                            textViewContributorTitle.text = viewState.subtitle

                            viewState.imageUrl?.let { imageUrl ->
                                imageLoader.load(
                                    imageViewPodcastEpisode,
                                    imageUrl,
                                    ImageLoaderOptions.Builder()
                                        .placeholderResId(R.drawable.ic_podcast_placeholder)
                                        .build()
                                )
                            }

                            imageViewForward30Button.goneIfFalse(!viewState.showLoading)
                            progressBarAudioLoading.goneIfFalse(viewState.showLoading)

                            binding.apply {
                                if (swipeRevealLayoutPlayer.isOpened) {
                                    swipeRevealLayoutPlayer.close(true)
                                }

                                imageViewPlayerBarShadow.visible
                                imageViewBottomBarShadow.gone

                                swipeRevealLayoutPlayer.visible
                            }

                            root.visible
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deepLinkPopupViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is DeepLinkPopupViewState.ExternalAuthorizePopup -> {
                        binding.layoutDashboardPopup.layoutDashboardAuthorizePopup.apply {
                            textViewDashboardPopupAuthorizeName.text = viewState.link.host
                            layoutConstraintAuthorizePopup.visible
                            root.visible
                        }
                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.StakworkAuthorizePopup -> {
                        binding.layoutDashboardPopup.layoutDashboardAuthorizePopup.apply {
                            textViewDashboardPopupAuthorizeName.text = viewState.link.host
                            layoutConstraintAuthorizePopup.visible
                            root.visible
                        }
                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.ExternalAuthorizePopupProcessing -> {
                        binding.layoutDashboardPopup.layoutDashboardAuthorizePopup.progressBarAuthorize.visible
                    }
                    is DeepLinkPopupViewState.LoadingExternalRequestPopup -> {
                        binding.layoutDashboardPopup.layoutDashboardPeopleProfilePopup.apply {
                            layoutConstraintLoadingProfile.visible
                            root.visible
                        }
                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.SaveProfilePopup -> {
                        binding.layoutDashboardPopup.layoutDashboardPeopleProfilePopup.apply {
                            layoutConstraintLoadingProfile.gone

                            textViewDashboardPopupPeopleProfileHost.text = viewState.host

                            textViewDashboardPopupPeopleProfileTitle.text = getString(R.string.dashboard_save_profile_popup_title)
                            buttonSaveProfile.text = getString(R.string.dashboard_save_profile_button)
                        }
                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.DeletePeopleProfilePopup -> {
                        binding.layoutDashboardPopup.layoutDashboardPeopleProfilePopup.apply {
                            layoutConstraintLoadingProfile.gone

                            textViewDashboardPopupPeopleProfileHost.text = viewState.host

                            textViewDashboardPopupPeopleProfileTitle.text = getString(R.string.dashboard_delete_profile_popup_title)
                            buttonSaveProfile.text = getString(R.string.dashboard_delete_profile_button)
                        }

                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.RedeemTokensPopup -> {
                        binding.layoutDashboardPopup.layoutDashboardPeopleProfilePopup.apply {
                            layoutConstraintLoadingProfile.gone

                            textViewDashboardPopupPeopleProfileHost.text = viewState.host

                            textViewDashboardPopupPeopleProfileTitle.text = getString(R.string.dashboard_redeem_badge_token_popup_title)
                            buttonSaveProfile.text = getString(R.string.dashboard_save_profile_button)
                        }
                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.ExternalRequestPopupProcessing -> {
                        binding.layoutDashboardPopup.layoutDashboardPeopleProfilePopup.progressBarSaveProfile.visible
                    }
                    is DeepLinkPopupViewState.PeopleConnectPopupLoadingPersonInfo -> {
                        disposable?.dispose()
                        imageJob?.cancel()

                        binding.layoutDashboardPopup.layoutDashboardConnectPopup.apply {
                            layoutConstraintDashboardConnectLoading.visible
                            root.visible
                        }
                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.PeopleConnectPopup -> {
                        disposable?.dispose()
                        imageJob?.cancel()

                        binding.layoutDashboardPopup.layoutDashboardConnectPopup.apply {

                            val alias = viewState.alias
                            textViewDashboardPeoplePopupName.text = alias

                            editTextDashboardPeoplePopupMessage.hint = getString(R.string.dashboard_connect_initial_message_hint, alias)
                            textViewDashboardPeoplePopupDescription.text = viewState.description

                            val priceToMeet = (viewState.priceToMeet).toSat()?.asFormattedString(appendUnit = true) ?: ""
                            textViewDashboardPeoplePopupPriceToMeet.text = getString(R.string.dashboard_connect_price_to_meet, priceToMeet)

                            viewState.photoUrl?.let { url ->

                                lifecycleScope.launch {
                                    imageLoader.load(
                                        imageViewProfilePicture,
                                        url,
                                        ImageLoaderOptions.Builder()
                                            .placeholderResId(R.drawable.ic_profile_avatar_circle)
                                            .transformation(Transformation.CircleCrop)
                                            .build()
                                    ).also {
                                        disposable = it
                                    }
                                }.let { job ->
                                    imageJob = job
                                }

                            } ?: imageViewProfilePicture.setImageDrawable(
                                ContextCompat.getDrawable(
                                    binding.root.context,
                                    R.drawable.ic_profile_avatar_circle
                                )
                            )

                            layoutConstraintDashboardConnectLoading.gone
                            root.visible
                        }
                        binding.layoutDashboardPopup.root.visible
                    }
                    is DeepLinkPopupViewState.PeopleConnectPopupProcessing -> {
                        binding.layoutDashboardPopup.layoutDashboardConnectPopup.progressBarConnect.visible
                    }
                    is DeepLinkPopupViewState.PopupDismissed -> {
                        binding.layoutDashboardPopup.layoutDashboardAuthorizePopup.apply {
                            root.gone
                            progressBarAuthorize.gone
                        }

                        binding.layoutDashboardPopup.layoutDashboardPeopleProfilePopup.apply {
                            root.gone
                            progressBarSaveProfile.gone
                        }

                        binding.layoutDashboardPopup.layoutDashboardConnectPopup.apply {
                            root.gone
                            progressBarConnect.gone
                        }

                        binding.layoutDashboardPopup.root.gone
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.chatListFooterButtonsViewStateContainer.collect { viewState ->
                binding.layoutDashboardNavDrawer.let { navDrawer ->
                    @Exhaustive
                    when (viewState) {
                        is ChatListFooterButtonsViewState.Idle -> {
                            navDrawer.layoutButtonAddFriend.layoutConstraintButtonAddFriend.gone
                            navDrawer.layoutButtonCreateTribe.layoutConstraintButtonCreateTribe.gone
                        }
                        is ChatListFooterButtonsViewState.ButtonsVisibility -> {
                            navDrawer.layoutButtonAddFriend.layoutConstraintButtonAddFriend.goneIfFalse(viewState.addFriendVisible)
                            navDrawer.layoutButtonCreateTribe.layoutConstraintButtonCreateTribe.goneIfFalse(viewState.createTribeVisible)
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreatedRestoreMotionScene(
        viewState: DashboardMotionViewState,
        binding: FragmentDashboardBinding
    ) {
        viewState.restoreMotionScene(binding.layoutMotionDashboard)
    }

    override fun getMotionLayouts(): Array<MotionLayout> {
        return arrayOf(binding.layoutMotionDashboard)
    }

    override suspend fun onSideEffectCollect(sideEffect: ChatListSideEffect) {
        sideEffect.execute(binding.root.context)
    }
}
