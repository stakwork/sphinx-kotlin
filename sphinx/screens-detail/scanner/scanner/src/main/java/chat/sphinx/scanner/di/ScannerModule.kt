package chat.sphinx.scanner.di

import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.scanner.coordinator.ScannerViewModelCoordinator
import chat.sphinx.scanner.navigation.ScannerNavigator
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object ScannerModule {

    @Provides
    @ActivityRetainedScoped
    fun provideScannerCoordinator(
        dispatchers: CoroutineDispatchers,
        scannerNavigator: ScannerNavigator,
        LOG: SphinxLogger,
    ): ScannerViewModelCoordinator =
        ScannerViewModelCoordinator(dispatchers, scannerNavigator, LOG)

    @Provides
    fun provideViewModelCoordinator(
        scannerViewModelCoordinator: ScannerViewModelCoordinator
    ): ViewModelCoordinator<ScannerRequest, ScannerResponse> =
        scannerViewModelCoordinator
}
