package chat.sphinx.feature_view_model_coordinator

import chat.sphinx.concept_view_model_coordinator.RequestHolder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RequestCatcher<BackType: Any, Request: Any, Response: Any>(
    private val viewModelScope: CoroutineScope,
    private val viewModelCoordinatorImpl: ViewModelCoordinatorImpl<BackType, Request, Response>,
    private val launchDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    @Suppress("RemoveExplicitTypeArguments")
    private val _caughtRequestsSharedFlow: MutableSharedFlow<MutableList<RequestHolder<Request>>> by lazy {
        MutableSharedFlow<MutableList<RequestHolder<Request>>>(1, 0)
    }

    suspend fun getCaughtRequestStateFlow(): StateFlow<List<RequestHolder<Request>>> =
        _caughtRequestsSharedFlow.map { it.toList() }.stateIn(viewModelScope)

    init {
        viewModelScope.launch(launchDispatcher) {

            viewModelCoordinatorImpl.getRequestSharedFlow().collect { request ->

                _caughtRequestsSharedFlow.replayCache.firstOrNull()?.let { list ->
                    list.add(request)
                    _caughtRequestsSharedFlow.emit(list)
                } ?: _caughtRequestsSharedFlow.emit(mutableListOf(request))
                
            }

        }
    }
}