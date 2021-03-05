package chat.sphinx.authentication.di

import chat.sphinx.authentication.components.SphinxAuthenticationViewCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object AuthenticationModule {

    @Provides
    fun provideAuthenticationCoordinator(
        sphinxAuthenticationViewCoordinator: SphinxAuthenticationViewCoordinator
    ): AuthenticationCoordinator =
        sphinxAuthenticationViewCoordinator
}
