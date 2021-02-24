package chat.sphinx.activitymain.di

import androidx.navigation.NavController
import chat.sphinx.activitymain.navigation.MainNavigationDriver
import chat.sphinx.activitymain.navigation.SplashNavigatorImpl
import chat.sphinx.annotation_hilt.navigation.MainDriver
import chat.sphinx.splash.navigation.SplashNavigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.components.ViewModelComponent
import io.matthewnelson.concept_navigation.BaseNavigationDriver

@Module
@InstallIn(ActivityRetainedComponent::class)
object ActivityRetainedModule {

    @Provides
    @MainDriver
    fun provideMainNavigationDriver(
        mainNavigationDriver: MainNavigationDriver
    ): BaseNavigationDriver<NavController> =
        mainNavigationDriver
}

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {

    @Provides
    fun provideSplashNavigator(
        splashNavigatorImpl: SplashNavigatorImpl
    ): SplashNavigator =
        splashNavigatorImpl
}
