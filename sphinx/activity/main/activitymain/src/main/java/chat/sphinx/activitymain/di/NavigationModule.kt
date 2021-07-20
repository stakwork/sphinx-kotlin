package chat.sphinx.activitymain.di

import androidx.navigation.NavController
import chat.sphinx.activitymain.navigation.drivers.AuthenticationNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.activitymain.navigation.navigators.detail.*
import chat.sphinx.activitymain.navigation.navigators.primary.*
import chat.sphinx.add_friend.navigation.AddFriendNavigator
import chat.sphinx.address_book.navigation.AddressBookNavigator
import chat.sphinx.camera.navigation.CameraNavigator
import chat.sphinx.chat_contact.navigation.ContactChatNavigator
import chat.sphinx.chat_group.navigation.GroupChatNavigator
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.contact_detail.navigation.ContactDetailNavigator
import chat.sphinx.create_tribe.navigation.CreateTribeNavigator
import chat.sphinx.dashboard.navigation.DashboardBottomNavBarNavigator
import chat.sphinx.dashboard.navigation.DashboardNavDrawerNavigator
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.hilt_qualifiers.AuthenticationDriver
import chat.sphinx.hilt_qualifiers.DetailDriver
import chat.sphinx.hilt_qualifiers.PrimaryDriver
import chat.sphinx.invite_friend.navigation.InviteFriendNavigator
import chat.sphinx.join_tribe.navigation.JoinTribeNavigator
import chat.sphinx.new_contact.navigation.NewContactNavigator
import chat.sphinx.onboard.navigation.OnBoardNavigator
import chat.sphinx.onboard_name.navigation.OnBoardNameNavigator
import chat.sphinx.onboard_ready.navigation.OnBoardReadyNavigator
import chat.sphinx.payment_receive.navigation.PaymentReceiveNavigator
import chat.sphinx.payment_send.navigation.PaymentSendNavigator
import chat.sphinx.podcast_player.navigation.PodcastPlayerNavigator
import chat.sphinx.profile.navigation.ProfileNavigator
import chat.sphinx.qr_code.navigation.QRCodeNavigator
import chat.sphinx.scanner.navigation.ScannerNavigator
import chat.sphinx.splash.navigation.SplashNavigator
import chat.sphinx.support_ticket.navigation.SupportTicketNavigator
import chat.sphinx.transactions.navigation.TransactionsNavigator
import chat.sphinx.tribe_detail.navigation.TribeDetailNavigator
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
        onBoardNavigatorImpl: OnBoardNavigatorImpl
    ): OnBoardNavigator =
        onBoardNavigatorImpl

    @Provides
    fun provideOnBoardNameNavigator(
        onBoardNameNavigatorImpl: OnBoardNameNavigatorImpl
    ): OnBoardNameNavigator =
        onBoardNameNavigatorImpl

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
    fun provideProfileNavigator(
        profileNavigatorImpl: ProfileNavigatorImpl
    ): ProfileNavigator =
        profileNavigatorImpl

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
    fun provideContactDetailNavigator(
        contactDetailNavigatorImpl: ContactDetailNavigatorImpl
    ): ContactDetailNavigator =
        contactDetailNavigatorImpl

}
