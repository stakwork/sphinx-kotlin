package chat.sphinx.concept_view_model_coordinator

import chat.sphinx.kotlin_response.Response
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates cross-module request/responses from ViewModel to ViewModel.
 *
 * This service object must be scoped to ActivityRetained (for Android) such
 * that the same instance can be shared across screen modules.
 * */
abstract class ViewModelCoordinator<Request: Any, Success: Any>(
    val handleMultipleRequests: Boolean
) {

    private val lock: Mutex by lazy {
        Mutex()
    }

    suspend fun submitRequest(request: Request): Response<Success, RequestCancelled<Request>> {
        return if (handleMultipleRequests) {
            submitRequestImpl(RequestHolder.instantiate(request))
        } else {
            lock.withLock {
                submitRequestImpl(RequestHolder.instantiate(request))
            }
        }
    }

    protected abstract suspend fun submitRequestImpl(
        holder: RequestHolder<Request>
    ): Response<Success, RequestCancelled<Request>>
}
