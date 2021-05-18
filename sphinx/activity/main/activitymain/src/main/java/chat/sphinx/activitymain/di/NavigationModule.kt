package chat.sphinx.activitymain.di

import androidx.navigation.NavController
import chat.sphinx.activitymain.navigation.navigators.detail.AddFriendNavigatorImpl
import chat.sphinx.activitymain.navigation.drivers.AuthenticationNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.activitymain.navigation.navigators.detail.NewContactNavigatorImpl
import chat.sphinx.activitymain.navigation.navigators.primary.*
import chat.sphinx.add_friend.navigation.AddFriendNavigator
import chat.sphinx.address_book.navigation.AddressBookNavigator
import chat.sphinx.chat_contact.navigation.ContactChatNavigator
import chat.sphinx.chat_group.navigation.GroupChatNavigator
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.dashboard.navigation.DashboardBottomNavBarNavigator
import chat.sphinx.dashboard.navigation.DashboardNavDrawerNavigator
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.hilt_qualifiers.AuthenticationDriver
import chat.sphinx.hilt_qualifiers.DetailDriver
import chat.sphinx.hilt_qualifiers.PrimaryDriver
import chat.sphinx.new_contact.navigation.NewContactNavigator
import chat.sphinx.onboard.navigation.OnBoardNavigator
import chat.sphinx.profile.navigation.ProfileNavigator
import chat.sphinx.splash.navigation.SplashNavigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.matthewnelson.concept_navigation.BaseNavigationDriver

@Module
@InstallIn(ActivityRetainedComponent::class)
object NavigationModule {

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

    //////////////////
    /// Navigators ///
    //////////////////
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
    fun provideOnBoardNavigator(
        onBoardNavigatorImpl: OnBoardNavigatorImpl
    ): OnBoardNavigator =
        onBoardNavigatorImpl

    @Provides
    fun provideSplashNavigator(
        splashNavigatorImpl: SplashNavigatorImpl
    ): SplashNavigator =
        splashNavigatorImpl

    @Provides
    fun provideAddFriendNavigator(
        addFriendNavigatorImpl: AddFriendNavigatorImpl
    ): AddFriendNavigator =
        addFriendNavigatorImpl

    @Provides
    fun provideNewContactNavigator(
        newContactNavigatorImpl: NewContactNavigatorImpl
    ): NewContactNavigator =
        newContactNavigatorImpl

    @Provides
    fun provideContactChatNavigator(
        contactChatNavigatorImpl: ContactChatNavigatorImpl
    ): ContactChatNavigator =
        contactChatNavigatorImpl

    @Provides
    fun provideGroupChatNavigator(
        groupChatNavigatorImpl: GroupChatNavigatorImpl
    ): GroupChatNavigator =
        groupChatNavigatorImpl

    @Provides
    fun provideTribeChatNavigator(
        tribeChatNavigatorImpl: TribeChatNavigatorImpl
    ): TribeChatNavigator =
        tribeChatNavigatorImpl

    @Provides
    fun provideAddressBookNavigator(
        addressBookNavigatorImpl: AddressBookNavigatorImpl
    ): AddressBookNavigator =
        addressBookNavigatorImpl

    @Provides
    fun provideProfileNavigator(
        profileNavigatorImpl: ProfileNavigatorImpl
    ): ProfileNavigator =
        profileNavigatorImpl
}
