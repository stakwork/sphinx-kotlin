package chat.sphinx.scanner.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_view_model_coordinator.RequestCancelled
import chat.sphinx.concept_view_model_coordinator.ResponseHolder
import chat.sphinx.feature_view_model_coordinator.RequestCatcher
import chat.sphinx.kotlin_response.Response
import chat.sphinx.scanner.coordinator.ScannerViewModelCoordinator
import chat.sphinx.scanner.navigation.BackType
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
internal class ScannerViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val scannerViewModelCoordinator: ScannerViewModelCoordinator,
    private val handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        NotifySideEffect,
        ScannerViewState
        >(
            dispatchers,
            handle.navArgs<ScannerFragmentArgs>().let {
                ScannerViewState.LayoutVisibility(
                    it.value.argShowBackArrow,
                    it.value.argShowBottomView,
                    it.value.argScannerModeLabel
                )
            },
        )
{
    // Instantiate immediately so the request is pulled in
    // from shared flow via the coordinator
    private val requestCatcher = RequestCatcher(
        viewModelScope,
        scannerViewModelCoordinator,
        mainImmediate
    )

    private var responseJob: Job? = null
    fun processResponse(scannerResponse: ScannerResponse) {
        if (responseJob?.isActive == true) {
            return
        }

        responseJob = viewModelScope.launch(mainImmediate) {

            try {
                requestCatcher.getCaughtRequestStateFlow().collect { list ->
                    list.firstOrNull()?.let { requestHolder ->

                        requestHolder.request.filter?.let { filter ->
                            val returned = withContext(default) {
                                filter.checkData(scannerResponse.value)
                            }

                            @Exhaustive
                            when (returned) {
                                is Response.Error -> {
                                    if (returned.cause.isNotEmpty()) {
                                        submitSideEffect(NotifySideEffect(returned.cause))
                                    }
                                    delay(1_000L)
                                    throw Exception()
                                }
                                is Response.Success -> {}
                            }

                        }

                        scannerViewModelCoordinator.submitResponse(
                            response = Response.Success(
                                ResponseHolder(
                                    requestHolder,
                                    scannerResponse
                                )
                            ),
                            navigateBack = BackType.PopBackStack
                        )
                    }
                }
            } catch (e: Exception) {}
        }
    }

    fun goBack(type: BackType) {
        if (responseJob?.isActive == true) {
            return
        }

        responseJob = viewModelScope.launch(mainImmediate) {
            // Scanner coordinator is setup for handling a single response at a time
            // so we're ok to collect which will be cancelled when navigating back
            // as the scope will be cancelled.
            requestCatcher.getCaughtRequestStateFlow().collect { list ->
                list.firstOrNull()?.let { requestHolder ->
                    scannerViewModelCoordinator.submitResponse(
                        response = Response.Error(RequestCancelled(requestHolder)),
                        navigateBack = type
                    )
                }
            }
        }
    }
}
