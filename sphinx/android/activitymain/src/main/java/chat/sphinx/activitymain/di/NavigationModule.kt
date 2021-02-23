package chat.sphinx.activitymain.di

import androidx.navigation.NavController
import chat.sphinx.activitymain.navigation.MainNavigationDriver
import chat.sphinx.annotation_hilt.navigation.MainDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import io.matthewnelson.concept_navigation.BaseNavigationDriver

@Module
@InstallIn(ActivityRetainedComponent::class)
object NavigationModule {

    @Provides
    @MainDriver
    fun provideMainNavigationDriver(
        mainNavigationDriver: MainNavigationDriver
    ): BaseNavigationDriver<NavController> =
        mainNavigationDriver
}
