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

    /**
     * Returning a [Success] from this method will immediately return the
     * response to the request submitter, and [submitRequest] will not
     * navigate to the destination specified by the implementing class.
     *
     * Return `null` to always navigate to the screen.
     * */
    protected abstract suspend fun checkRequest(request: Request): Success?

    suspend fun submitRequest(request: Request): Response<Success, RequestCancelled<Request>> {
        checkRequest(request)?.let {
            return Response.Success(it)
        }

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
