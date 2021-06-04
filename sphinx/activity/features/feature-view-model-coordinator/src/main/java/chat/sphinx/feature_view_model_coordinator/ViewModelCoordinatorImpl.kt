package chat.sphinx.feature_view_model_coordinator

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_view_model_coordinator.RequestCancelled
import chat.sphinx.concept_view_model_coordinator.RequestHolder
import chat.sphinx.concept_view_model_coordinator.ResponseHolder
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.collections.ArrayList

abstract class ViewModelCoordinatorImpl<BackType: Any, Request: Any, Success: Any>(
    protected val LOG: SphinxLogger,
    handleMultipleRequests: Boolean = DEFAULT_HANDLE_MULTIPLE_REQUESTS,
    protected val navigateBackDelay: Long = DEFAULT_NAVIGATE_BACK_DELAY,
    protected val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
): ViewModelCoordinator<Request, Success>(handleMultipleRequests) {

    companion object {
        const val DEFAULT_HANDLE_MULTIPLE_REQUESTS = false
        const val DEFAULT_NAVIGATE_BACK_DELAY = 100L
    }

    init {
        require(navigateBackDelay >= DEFAULT_NAVIGATE_BACK_DELAY) {
            "Parameter navigateBackDelay must be >= $DEFAULT_NAVIGATE_BACK_DELAY"
        }
    }

    @Suppress("PropertyName")
    protected val TAG: String by lazy {
        this.javaClass.simpleName
    }

    @Suppress("PropertyName", "RemoveExplicitTypeArguments")
    private val _requestSharedFlow: MutableSharedFlow<RequestHolder<Request>> by lazy {
        MutableSharedFlow<RequestHolder<Request>>(0, 1)
    }

    @Suppress("RemoveExplicitTypeArguments")
    private val catcherAvailabilityStateFlow: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow<Boolean>(false)
    }

    @Suppress("PropertyName")
    private val _responseSharedFlow: MutableSharedFlow<
            /*              Success                           Error            */
            Response<ResponseHolder<Request, Success>, RequestCancelled<Request>>
            >
    by lazy { MutableSharedFlow(0, 1) }

    protected abstract suspend fun navigateToScreen(request: RequestHolder<Request>)

    override suspend fun submitRequestImpl(
        holder: RequestHolder<Request>
    ): Response<Success, RequestCancelled<Request>> {
        navigateToScreen(holder)

        var response: Response<Success, RequestCancelled<Request>>? = null

        try {
            catcherAvailabilityStateFlow.collect { isAvailable ->
                if (isAvailable) {

                    delay(25L)

                    _requestSharedFlow.emit(holder)

                    _responseSharedFlow.collect { responseFlow ->
                        @Exhaustive
                        when (responseFlow) {
                            is Response.Error -> {
                                if (responseFlow.cause.requestHolder.uuid == holder.uuid) {
                                    response = responseFlow
                                }
                            }
                            is Response.Success -> {
                                if (responseFlow.value.requestHolder.uuid == holder.uuid) {
                                    response = Response.Success(responseFlow.value.response)
                                }
                            }
                        }

                        throw Exception()
                    }
                }
            }
        } catch (e: Exception) {}

        delay(navigateBackDelay)

        return response ?: Response.Error(RequestCancelled(holder))
    }

    suspend fun getRequestSharedFlow(): SharedFlow<RequestHolder<Request>> {
        val supervisor = SupervisorJob(currentCoroutineContext().job)
        val newScope = CoroutineScope(supervisor)

        val requests = ArrayList<RequestHolder<Request>>(1)
        val flow = _requestSharedFlow.asSharedFlow()

        newScope.launch(dispatcher) {
            flow.collect { requestHolder ->
                requests.add(requestHolder)
            }
        }.invokeOnCompletion {
            if (_responseSharedFlow.subscriptionCount.value > 0) {
                // OK to use GlobalScope here as it's uncancelable and
                // we want to make sure all jobs are completed.
                @Suppress("EXPERIMENTAL_API_USAGE")
                GlobalScope.launch(dispatcher) {
                    LOG.d(TAG, "invokeOnCompletion emitting cancellation responses")
                    for (requestHolder in requests) {
                        _responseSharedFlow.emit(
                            Response.Error(RequestCancelled(requestHolder))
                        )
                    }
                }
            }
            catcherAvailabilityStateFlow.value = false
        }

        catcherAvailabilityStateFlow.value = true
        return _requestSharedFlow.asSharedFlow()
    }

    protected abstract suspend fun navigateBack(back: BackType)

    /**
     * If [navigateBack] is `null`, will _not_ navigate back. This is primarily for
     * submission of multiple requests if [handleMultipleRequests] is `true`.
     * */
    suspend fun submitResponse(
        response: Response<ResponseHolder<Request, Success>, RequestCancelled<Request>>,
        navigateBack: BackType?,
    ) {
        _responseSharedFlow.emit(response)

        if (navigateBack != null) {
            navigateBack(navigateBack)
            delay(navigateBackDelay)
        }
    }
}
