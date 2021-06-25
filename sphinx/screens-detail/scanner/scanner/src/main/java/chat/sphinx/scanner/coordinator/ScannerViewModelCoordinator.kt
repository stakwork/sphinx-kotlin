package chat.sphinx.scanner.coordinator

import chat.sphinx.concept_view_model_coordinator.RequestHolder
import chat.sphinx.feature_view_model_coordinator.ViewModelCoordinatorImpl
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.scanner.navigation.BackType
import chat.sphinx.scanner.navigation.ScannerNavigator
import chat.sphinx.scanner_view_model_coordinator.request.ScannerRequest
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class ScannerViewModelCoordinator(
    dispatchers: CoroutineDispatchers,
    private val scannerNavigator: ScannerNavigator,
    LOG: SphinxLogger,
): ViewModelCoordinatorImpl<BackType, ScannerRequest, ScannerResponse>(
    LOG = LOG,
    dispatcher = dispatchers.mainImmediate
) {
    override suspend fun navigateBack(back: BackType) {
        when (back) {
            is BackType.CloseDetailScreen -> {
                scannerNavigator.closeDetailScreen()
            }
            is BackType.PopBackStack -> {
                scannerNavigator.popBackStack()
            }
        }
    }

    override suspend fun navigateToScreen(request: RequestHolder<ScannerRequest>) {
        scannerNavigator.toScannerScreen(request.request.showBottomView, request.request.scannerModeLabel)
    }

    override suspend fun checkRequest(request: ScannerRequest): ScannerResponse? {
        return null
    }
}
