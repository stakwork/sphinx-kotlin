package chat.sphinx.tribes_discover.coordinator

import chat.sphinx.concept_view_model_coordinator.RequestHolder
import chat.sphinx.feature_view_model_coordinator.ViewModelCoordinatorImpl
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.tribes_discover.navigation.BackType
import chat.sphinx.tribes_discover.navigation.TribesDiscoverNavigator
import chat.sphinx.tribes_discover_view_model_coordinator.request.TribesDiscoverRequest
import chat.sphinx.tribes_discover_view_model_coordinator.response.TribesDiscoverResponse
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class TribesDiscoverViewModelCoordinator(
    dispatchers: CoroutineDispatchers,
    private val tribesDiscoverNavigator: TribesDiscoverNavigator,
    LOG: SphinxLogger,
): ViewModelCoordinatorImpl<BackType, TribesDiscoverRequest, TribesDiscoverResponse>(
    LOG = LOG,
    dispatcher = dispatchers.mainImmediate
) {
    override suspend fun navigateBack(back: BackType) {
        when (back) {
            is BackType.CloseDetailScreen -> {
                tribesDiscoverNavigator.closeDetailScreen()
            }
            is BackType.PopBackStack -> {
                tribesDiscoverNavigator.popBackStack()
            }
        }
    }

    override suspend fun navigateToScreen(request: RequestHolder<TribesDiscoverRequest>) {
        tribesDiscoverNavigator.toTribesDiscover()
    }

    override suspend fun checkRequest(request: TribesDiscoverRequest): TribesDiscoverResponse? {
        return null
    }
}
