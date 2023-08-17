package chat.sphinx.activitymain.di

import androidx.navigation.NavController
import chat.sphinx.activitymain.navigation.drivers.AuthenticationNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.activitymain.navigation.navigators.detail.*
import chat.sphinx.activitymain.navigation.navigators.primary.*
import chat.sphinx.add_friend.navigation.AddFriendNavigator
import chat.sphinx.add_tribe_member.navigation.AddTribeMemberNavigator
import chat.sphinx.address_book.navigation.AddressBookNavigator
import chat.sphinx.camera.navigation.CameraNavigator
import chat.sphinx.chat_contact.navigation.ContactChatNavigator
import chat.sphinx.chat_group.navigation.GroupChatNavigator
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.common_player.navigation.CommonPlayerNavigator
import chat.sphinx.create_badge.navigation.CreateBadgeNavigator
import chat.sphinx.create_tribe.navigation.CreateTribeNavigator
import chat.sphinx.dashboard.navigation.DashboardBottomNavBarNavigator
import chat.sphinx.dashboard.navigation.DashboardNavDrawerNavigator
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.delete_chat_media.navigation.DeleteChatMediaNavigator
import chat.sphinx.tribes_discover.navigation.TribesDiscoverNavigator
import chat.sphinx.edit_contact.navigation.EditContactNavigator
import chat.sphinx.episode_description.navigation.EpisodeDescriptionNavigator
import chat.sphinx.episode_detail.navigation.EpisodeDetailNavigator
import chat.sphinx.example.delete_chat_media_detail.navigation.DeleteChatMediaDetailNavigator
import chat.sphinx.example.delete_media.navigation.DeleteMediaNavigator
import chat.sphinx.example.delete_media_detail.navigation.DeleteMediaDetailNavigator
import chat.sphinx.example.manage_storage.navigation.ManageStorageNavigator
import chat.sphinx.hilt_qualifiers.AuthenticationDriver
import chat.sphinx.hilt_qualifiers.DetailDriver
import chat.sphinx.hilt_qualifiers.PrimaryDriver
import chat.sphinx.invite_friend.navigation.InviteFriendNavigator
import chat.sphinx.join_tribe.navigation.JoinTribeNavigator
import chat.sphinx.known_badges.navigation.KnownBadgesNavigator
import chat.sphinx.new_contact.navigation.NewContactNavigator
import chat.sphinx.newsletter_detail.navigation.NewsletterDetailNavigator
import chat.sphinx.notification_level.navigation.NotificationLevelNavigator
import chat.sphinx.onboard_description.navigation.OnBoardDescriptionNavigator
import chat.sphinx.onboard.navigation.OnBoardMessageNavigator
import chat.sphinx.onboard_connect.navigation.OnBoardConnectNavigator
import chat.sphinx.onboard_connected.navigation.OnBoardConnectedNavigator
import chat.sphinx.onboard_connecting.navigation.OnBoardConnectingNavigator
import chat.sphinx.onboard_desktop.navigation.OnBoardDesktopNavigator
import chat.sphinx.onboard_lightning.navigation.OnBoardLightningNavigator
import chat.sphinx.onboard_name.navigation.OnBoardNameNavigator
import chat.sphinx.onboard_picture.navigation.OnBoardPictureNavigator
import chat.sphinx.onboard_ready.navigation.OnBoardReadyNavigator
import chat.sphinx.onboard_welcome.navigation.OnBoardWelcomeNavigator
import chat.sphinx.payment_receive.navigation.PaymentReceiveNavigator
import chat.sphinx.payment_send.navigation.PaymentSendNavigator
import chat.sphinx.payment_template.navigation.PaymentTemplateNavigator
import chat.sphinx.podcast_player.navigation.PodcastPlayerNavigator
import chat.sphinx.profile.navigation.ProfileNavigator
import chat.sphinx.qr_code.navigation.QRCodeNavigator
import chat.sphinx.scanner.navigation.ScannerNavigator
import chat.sphinx.splash.navigation.SplashNavigator
import chat.sphinx.subscription.navigation.SubscriptionNavigator
import chat.sphinx.support_ticket.navigation.SupportTicketNavigator
import chat.sphinx.threads.navigation.ThreadsNavigator
import chat.sphinx.transactions.navigation.TransactionsNavigator
import chat.sphinx.tribe_badge.navigation.TribeBadgesNavigator
import chat.sphinx.tribe_detail.navigation.TribeDetailNavigator
import chat.sphinx.tribe_members_list.navigation.TribeMembersListNavigator
import chat.sphinx.video_screen.navigation.VideoScreenNavigator
import chat.sphinx.web_view.navigation.WebViewNavigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.matthewnelson.concept_navigation.BaseNavigationDriver

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object NavigationModule {

    ///////////////
    /// Drivers ///
    ///////////////
    @Provides
    @ActivityRetainedScoped
    fun provideAuthenticationNavigationDriver(): AuthenticationNavigationDriver =
        AuthenticationNavigationDriver()

    @Provides
    @AuthenticationDriver
    fun provideAuthenticationBaseNavigationDriver(
        authenticationNavigationDriver: AuthenticationNavigationDriver
    ): BaseNavigationDriver<NavController> =
        authenticationNavigationDriver

    @Provides
    @ActivityRetainedScoped
    fun provideDetailNavigationDriver(): DetailNavigationDriver =
        DetailNavigationDriver()

    @Provides
    @DetailDriver
    fun provideDetailBaseNavigationDriver(
        detailNavigationDriver: DetailNavigationDriver
    ): BaseNavigationDriver<NavController> =
        detailNavigationDriver

    @Provides
    @ActivityRetainedScoped
    fun providePrimaryNavigationDriver(): PrimaryNavigationDriver =
        PrimaryNavigationDriver()

    @Provides
    @PrimaryDriver
    fun providePrimaryBaseNavigationDriver(
        primaryNavigationDriver: PrimaryNavigationDriver
    ): BaseNavigationDriver<NavController> =
        primaryNavigationDriver

    //////////////////////////
    /// Primary Navigators ///
    //////////////////////////
    @Provides
    fun provideAddressBookNavigator(
        addressBookNavigatorImpl: AddressBookNavigatorImpl
    ): AddressBookNavigator =
        addressBookNavigatorImpl

    @Provides
    fun provideContactChatNavigator(
        contactChatNavigatorImpl: ContactChatNavigatorImpl
    ): ContactChatNavigator =
        contactChatNavigatorImpl

    @Provides
    fun provideDashboardBottomNavBarNavigator(
        dashboardBottomNavBarNavigatorImpl: DashboardBottomNavBarNavigatorImpl
    ): DashboardBottomNavBarNavigator =
        dashboardBottomNavBarNavigatorImpl

    @Provides
    fun provideDashboardNavDrawerNavigator(
        dashboardNavDrawerNavigatorImpl: DashboardNavDrawerNavigatorImpl
    ): DashboardNavDrawerNavigator =
        dashboardNavDrawerNavigatorImpl

    @Provides
    fun provideDashboardNavigator(
        dashboardNavigatorImpl: DashboardNavigatorImpl
    ): DashboardNavigator =
        dashboardNavigatorImpl

    @Provides
    fun provideGroupChatNavigator(
        groupChatNavigatorImpl: GroupChatNavigatorImpl
    ): GroupChatNavigator =
        groupChatNavigatorImpl

    @Provides
    fun provideOnBoardNavigator(
        onBoardNavigatorImpl: OnBoardMessageNavigatorImpl
    ): OnBoardMessageNavigator =
        onBoardNavigatorImpl

    @Provides
    fun provideOnBoardWelcomeNavigator(
        onBoardWelcomeNavigatorImpl: OnBoardWelcomeNavigatorImpl
    ): OnBoardWelcomeNavigator =
        onBoardWelcomeNavigatorImpl

    @Provides
    fun provideOnBoardDescriptionNavigator(
        onBoardDescriptionNavigatorImpl: OnBoardDescriptionNavigatorImpl
    ): OnBoardDescriptionNavigator =
        onBoardDescriptionNavigatorImpl

    @Provides
    fun provideOnBoardConnectNavigator(
        onBoardConnectNavigatorImpl: OnBoardConnectNavigatorImpl
    ): OnBoardConnectNavigator =
        onBoardConnectNavigatorImpl

    @Provides
    fun provideOnBoardConnectingNavigator(
        onBoardConnectingNavigatorImpl: OnBoardConnectingNavigatorImpl
    ): OnBoardConnectingNavigator =
        onBoardConnectingNavigatorImpl

    @Provides
    fun provideOnBoardConnectedNavigator(
        onBoardConnectedNavigatorImpl: OnBoardConnectedNavigatorImpl
    ): OnBoardConnectedNavigator =
        onBoardConnectedNavigatorImpl

    @Provides
    fun provideOnBoardLightningNavigator(
        onBoardLightningNavigatorImpl: OnBoardLightningNavigatorImpl
    ): OnBoardLightningNavigator =
        onBoardLightningNavigatorImpl

    @Provides
    fun provideOnBoardDesktopNavigator(
        onBoardDesktopNavigatorImpl: OnBoardDesktopNavigatorImpl
    ): OnBoardDesktopNavigator =
        onBoardDesktopNavigatorImpl

    @Provides
    fun provideOnBoardNameNavigator(
        onBoardNameNavigatorImpl: OnBoardNameNavigatorImpl
    ): OnBoardNameNavigator =
        onBoardNameNavigatorImpl

    @Provides
    fun provideOnBoardPictureNavigator(
        onBoardPictureNavigatorImpl: OnBoardPictureNavigatorImpl
    ): OnBoardPictureNavigator =
        onBoardPictureNavigatorImpl

    @Provides
    fun provideOnBoardReadyNavigator(
        onBoardReadyNavigatorImpl: OnBoardReadyNavigatorImpl
    ): OnBoardReadyNavigator =
        onBoardReadyNavigatorImpl

    @Provides
    fun provideSplashNavigator(
        splashNavigatorImpl: SplashNavigatorImpl
    ): SplashNavigator =
        splashNavigatorImpl

    @Provides
    fun provideTribeChatNavigator(
        tribeChatNavigatorImpl: TribeChatNavigatorImpl
    ): TribeChatNavigator =
        tribeChatNavigatorImpl

    ////////////////////////////////
    /// Detail Screen Navigators ///
    ////////////////////////////////
    @Provides
    fun provideAddFriendNavigator(
        addFriendNavigatorImpl: AddFriendNavigatorImpl
    ): AddFriendNavigator =
        addFriendNavigatorImpl

    @Provides
    fun provideCameraNavigator(
        cameraNavigatorImpl: CameraNavigatorImpl
    ): CameraNavigator =
        cameraNavigatorImpl

    @Provides
    fun provideCreateTribeNavigator(
        createTribeNavigatorImpl: CreateTribeNavigatorImpl
    ): CreateTribeNavigator =
        createTribeNavigatorImpl

    @Provides
    fun provideNewContactNavigator(
        newContactNavigatorImpl: NewContactNavigatorImpl
    ): NewContactNavigator =
        newContactNavigatorImpl

    @Provides
    fun provideEditContactNavigator(
        editContactNavigatorImpl: EditContactNavigatorImpl
    ): EditContactNavigator =
        editContactNavigatorImpl

    @Provides
    fun providePaymentReceiveNavigator(
        paymentReceiveNavigatorImpl: PaymentReceiveNavigatorImpl
    ): PaymentReceiveNavigator =
        paymentReceiveNavigatorImpl

    @Provides
    fun providePaymentSendNavigator(
        paymentSendNavigatorImpl: PaymentSendNavigatorImpl
    ): PaymentSendNavigator =
        paymentSendNavigatorImpl

    @Provides
    fun providePaymentTemplateNavigator(
        paymentTemplateNavigatorImpl: PaymentTemplateNavigatorImpl
    ): PaymentTemplateNavigator =
        paymentTemplateNavigatorImpl

    @Provides
    fun provideScannerNavigator(
        scannerNavigatorImpl: ScannerNavigatorImpl
    ): ScannerNavigator =
        scannerNavigatorImpl

    @Provides
    fun provideQRCodeNavigator(
        qrCodeNavigatorImpl: QRCodeNavigatorImpl
    ): QRCodeNavigator =
        qrCodeNavigatorImpl

    @Provides
    fun provideManageStorageNavigator(
        manageStorageNavigatorImpl: ManageStorageNavigatorImpl
    ): ManageStorageNavigator =
        manageStorageNavigatorImpl

    @Provides
    fun provideThreadsNavigator(
        threadsNavigatorImpl: ThreadsNavigatorImpl
    ): ThreadsNavigator =
        threadsNavigatorImpl

    @Provides
    fun provideDeleteMediaNavigator(
        deleteMediaNavigatorImpl: DeleteMediaNavigatorImpl
    ): DeleteMediaNavigator =
        deleteMediaNavigatorImpl

    @Provides
    fun provideDeleteDetailMediaNavigator(
        deleteMediaDetailNavigatorImpl: DeleteMediaDetailNavigatorImpl
    ): DeleteMediaDetailNavigator =
        deleteMediaDetailNavigatorImpl

    @Provides
    fun provideDeleteChatMediaNavigator(
        deleteChatMediaNavigatorImpl: DeleteChatMediaNavigatorImpl
    ): DeleteChatMediaNavigator =
        deleteChatMediaNavigatorImpl

    @Provides
    fun provideDeleteChatMediaDetailNavigator(
        deleteChatMediaDetailNavigatorImpl: DeleteChatMediaDetailNavigatorImpl
    ): DeleteChatMediaDetailNavigator =
        deleteChatMediaDetailNavigatorImpl

    @Provides
    fun provideProfileNavigator(
        profileNavigatorImpl: ProfileNavigatorImpl
    ): ProfileNavigator =
        profileNavigatorImpl

    @Provides
    fun provideSubscriptionNavigator(
        subscriptionNavigatorImpl: SubscriptionNavigatorImpl
    ): SubscriptionNavigator =
        subscriptionNavigatorImpl

    @Provides
    fun provideSupportTicketNavigator(
        supportTicketNavigatorImpl: SupportTicketNavigatorImpl
    ): SupportTicketNavigator =
        supportTicketNavigatorImpl

    @Provides
    fun provideTransactionsNavigator(
        transactionsNavigatorImpl: TransactionsNavigatorImpl
    ): TransactionsNavigator =
        transactionsNavigatorImpl

    @Provides
    fun provideTribeMembersListNavigator(
        tribeMembersListNavigatorImpl: TribeMembersListNavigatorImpl
    ): TribeMembersListNavigator =
        tribeMembersListNavigatorImpl

    @Provides
    fun provideAddTribeMemberNavigator(
        addTribeMemberNavigatorImpl: AddTribeMemberNavigatorImpl
    ): AddTribeMemberNavigator =
        addTribeMemberNavigatorImpl

    @Provides
    fun provideDiscoverTribesNavigator(
        discoverTribesNavigatorImpl: TribesDiscoverNavigatorImpl
    ): TribesDiscoverNavigator =
        discoverTribesNavigatorImpl

    @Provides
    fun provideTribeBadgesNavigator(
        tribeBadgesNavigatorImpl: TribeBadgesNavigatorImpl
    ): TribeBadgesNavigator =
        tribeBadgesNavigatorImpl

    @Provides
    fun provideCreateBadgeNavigator(
        createBadgeNavigatorImpl: CreateBadgeNavigatorImpl
    ): CreateBadgeNavigator =
        createBadgeNavigatorImpl

    @Provides
    fun provideJoinTribeNavigator(
        joinTribeNavigatorImpl: JoinTribeNavigatorImpl
    ): JoinTribeNavigator =
        joinTribeNavigatorImpl

    @Provides
    fun provideTribeChatPodcastPlayerNavigator(
        tribeChatPodcastPlayerNavigatorImpl: PodcastPlayerNavigatorImpl
    ): PodcastPlayerNavigator =
        tribeChatPodcastPlayerNavigatorImpl

    @Provides
    fun provideInviteFriendNavigator(
        inviteFriendNavigatorImpl: InviteFriendNavigatorImpl
    ): InviteFriendNavigator =
        inviteFriendNavigatorImpl

    @Provides
    fun provideTribeDetailNavigator(
        tribeDetailNavigatorImpl: TribeDetailNavigatorImpl
    ): TribeDetailNavigator =
        tribeDetailNavigatorImpl

    @Provides
    fun provideVideoScreenNavigator(
        videoScreenNavigatorImpl: VideoScreenNavigatorImpl
    ): VideoScreenNavigator =
        videoScreenNavigatorImpl

    @Provides
    fun provideWebViewNavigator(
        webViewNavigatorImpl: WebViewNavigatorImpl
    ): WebViewNavigator =
        webViewNavigatorImpl

    @Provides
    fun provideNewsletterDetailNavigator(
        newsletterDetailNavigatorImpl: NewsletterDetailNavigatorImpl
    ): NewsletterDetailNavigator =
        newsletterDetailNavigatorImpl

    @Provides
    fun provideCommonPlayerScreenNavigator(
        commonPlayerNavigatorImpl: CommonPlayerNavigatorImpl
    ): CommonPlayerNavigator =
        commonPlayerNavigatorImpl

    @Provides
    fun provideNotificationLevelNavigator(
        notificationLevelNavigatorImpl: NotificationLevelNavigatorImpl
    ): NotificationLevelNavigator =
        notificationLevelNavigatorImpl

    @Provides
    fun provideEpisodeDetailNavigator(
        episodeNavigatorNavigatorImpl: EpisodeDetailNavigatorImpl
    ): EpisodeDetailNavigator =
        episodeNavigatorNavigatorImpl

    @Provides
    fun provideKnownBadgesNavigator(
        knownBadgesNavigatorImpl: KnownBadgesNavigatorImpl
    ): KnownBadgesNavigator =
        knownBadgesNavigatorImpl

    @Provides
    fun provideEpisodeDescriptionNavigator(
        episodeDescriptionNavigatorImpl: EpisodeDescriptionNavigatorImpl
    ): EpisodeDescriptionNavigator =
        episodeDescriptionNavigatorImpl

}
