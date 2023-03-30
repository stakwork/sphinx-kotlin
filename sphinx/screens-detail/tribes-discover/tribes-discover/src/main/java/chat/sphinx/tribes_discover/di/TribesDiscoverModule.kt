package chat.sphinx.tribes_discover.di

import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.tribes_discover.coordinator.TribesDiscoverViewModelCoordinator
import chat.sphinx.tribes_discover.navigation.TribesDiscoverNavigator
import chat.sphinx.tribes_discover_view_model_coordinator.request.TribesDiscoverRequest
import chat.sphinx.tribes_discover_view_model_coordinator.response.TribesDiscoverResponse
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object TribesDiscoverModule {

    @Provides
    @ActivityRetainedScoped
    fun provideTribesDiscoverCoordinator(
        dispatchers: CoroutineDispatchers,
        scannerNavigator: TribesDiscoverNavigator,
        LOG: SphinxLogger,
    ): TribesDiscoverViewModelCoordinator =
        TribesDiscoverViewModelCoordinator(dispatchers, scannerNavigator, LOG)

    @Provides
    fun provideViewModelCoordinator(
        tribesDiscoverViewModelCoordinator: TribesDiscoverViewModelCoordinator
    ): ViewModelCoordinator<TribesDiscoverRequest, TribesDiscoverResponse> =
        tribesDiscoverViewModelCoordinator
}
