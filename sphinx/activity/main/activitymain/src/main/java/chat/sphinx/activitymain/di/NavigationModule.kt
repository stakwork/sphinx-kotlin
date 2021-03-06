package chat.sphinx.activitymain.di

import androidx.navigation.NavController
import chat.sphinx.activitymain.navigation.*
import chat.sphinx.dashboard.navigation.DashboardBottomNavBarNavigator
import chat.sphinx.dashboard.navigation.DashboardNavDrawerNavigator
import chat.sphinx.dashboard.navigation.DashboardNavigator
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

    @Provides
    fun provideBaseNavigationDriver(
        mainNavigationDriver: MainNavigationDriver
    ): BaseNavigationDriver<NavController> =
        mainNavigationDriver

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
}
