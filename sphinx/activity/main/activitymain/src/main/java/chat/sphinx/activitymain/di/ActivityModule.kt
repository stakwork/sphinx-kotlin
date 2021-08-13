package chat.sphinx.activitymain.di

import android.content.Context
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.user_colors_helper.UserColorsHelperImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

    @Provides
    @ActivityScoped
    fun provideUserColorsImpl(
        @ApplicationContext appContext: Context,
        dispatchers: CoroutineDispatchers
    ): UserColorsHelperImpl =
        UserColorsHelperImpl(appContext, dispatchers)

    @Provides
    fun provideUserColors(
        userColorsHelperImpl: UserColorsHelperImpl
    ): UserColorsHelper =
        userColorsHelperImpl
}
