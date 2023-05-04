package chat.sphinx.chat_tribe.ui

import android.app.Application
import android.content.Context
import android.webkit.JavascriptInterface
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_tribe.model.SphinxWebViewDto
import chat.sphinx.chat_tribe.ui.viewstate.TribeFeedViewState
import chat.sphinx.wrapper_chat.AppUrl
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class TribeAppViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
    private val app: Application,
    private val moshi: Moshi
    ) : BaseViewModel<TribeFeedViewState>(dispatchers, TribeFeedViewState.Idle) {

    @Volatile
    private var initialized: Boolean = false
    private var appUrl: AppUrl? = null

    private val _sphinxWebViewDtoStateFlow: MutableStateFlow<SphinxWebViewDto?> by lazy {
        MutableStateFlow(null)
    }

    val sphinxWebViewDtoStateFlow: StateFlow<SphinxWebViewDto?>
        get() = _sphinxWebViewDtoStateFlow.asStateFlow()


    fun init(url: AppUrl) {
        if (initialized) {
            return
        } else {
            initialized = true
            appUrl = url
        }
    }

    fun createSphinxWebViewDto(data: String) {
        viewModelScope.launch(mainImmediate) {
            withContext(default) {
                if (data.contains("type")) {
                    try {
                        _sphinxWebViewDtoStateFlow.value =
                            moshi.adapter(SphinxWebViewDto::class.java).fromJson(data)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    @JavascriptInterface
    fun receiveMessage(data: String) {
        createSphinxWebViewDto(data)
    }


}