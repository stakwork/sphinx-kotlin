package chat.sphinx.activitymain.di

import androidx.navigation.NavController
import chat.sphinx.activitymain.navigation.MainNavigationDriver
import chat.sphinx.activitymain.navigation.OnBoardNavigatorImpl
import chat.sphinx.activitymain.navigation.SplashNavigatorImpl
import chat.sphinx.onboard.navigation.OnBoardNavigator
import chat.sphinx.splash.navigation.SplashNavigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.components.ViewModelComponent
import io.matthewnelson.concept_navigation.BaseNavigationDriver

@Module
@InstallIn(ActivityRetainedComponent::class)
object NavigationModule {

    @Provides
    fun provideMainNavigationDriver(
        mainNavigationDriver: MainNavigationDriver
    ): BaseNavigationDriver<NavController> =
        mainNavigationDriver

    @Provides
    fun provideSplashNavigator(
        splashNavigatorImpl: SplashNavigatorImpl
    ): SplashNavigator =
        splashNavigatorImpl

    @Provides
    fun provideOnBoardNavigator(
        onBoardNavigatorImpl: OnBoardNavigatorImpl
    ): OnBoardNavigator =
        onBoardNavigatorImpl
}
