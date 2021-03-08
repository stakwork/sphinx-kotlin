package chat.sphinx.activitymain.di

import androidx.navigation.NavController
import chat.sphinx.activitymain.navigation.*
import chat.sphinx.activitymain.navigation.drivers.AuthenticationNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.add_friend.navigation.AddFriendNavigator
import chat.sphinx.dashboard.navigation.DashboardBottomNavBarNavigator
import chat.sphinx.dashboard.navigation.DashboardNavDrawerNavigator
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.hilt_qualifiers.AuthenticationDriver
import chat.sphinx.hilt_qualifiers.DetailDriver
import chat.sphinx.hilt_qualifiers.PrimaryDriver
import chat.sphinx.onboard.navigation.OnBoardNavigator
import chat.sphinx.splash.navigation.SplashNavigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import io.matthewnelson.concept_navigation.BaseNavigationDriver

@Module
@InstallIn(ActivityRetainedComponent::class)
object NavigationModule {

    ///////////////
    /// Drivers ///
    ///////////////
    @Provides
    @AuthenticationDriver
    fun provideAuthenticationBaseNavigationDriver(
        authenticationNavigationDriver: AuthenticationNavigationDriver
    ): BaseNavigationDriver<NavController> =
        authenticationNavigationDriver

    @Provides
    @DetailDriver
    fun provideDetailBaseNavigationDriver(
        detailNavigationDriver: DetailNavigationDriver
    ): BaseNavigationDriver<NavController> =
        detailNavigationDriver

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
}
