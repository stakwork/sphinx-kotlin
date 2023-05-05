package chat.sphinx.chat_tribe.ui

import android.app.Application
import android.webkit.JavascriptInterface
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_tribe.model.SendAuth
import chat.sphinx.chat_tribe.model.SphinxWebViewDto
import chat.sphinx.chat_tribe.model.generateRandomPass
import chat.sphinx.chat_tribe.model.generateSendAuthString
import chat.sphinx.chat_tribe.ui.viewstate.TribeFeedViewState
import chat.sphinx.chat_tribe.ui.viewstate.WebViewViewState
import chat.sphinx.concept_network_query_meme_server.NetworkQueryMemeServer
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.wrapper_chat.AppUrl
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class TribeAppViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
    private val networkQueryMemeServer: NetworkQueryMemeServer,
    private val contactRepository: ContactRepository,
    private val app: Application,
    private val moshi: Moshi
    ) : BaseViewModel<TribeFeedViewState>(dispatchers, TribeFeedViewState.Idle) {

    @Volatile
    private var initialized: Boolean = false
    private var appUrl: AppUrl? = null

    private var challenge: String? = null

    private val _sphinxWebViewDtoStateFlow: MutableStateFlow<SphinxWebViewDto?> by lazy {
        MutableStateFlow(null)
    }

    val webViewViewStateContainer: ViewStateContainer<WebViewViewState> by lazy {
        ViewStateContainer(WebViewViewState.Idle)
    }

    init {
        handleWebAppJson()
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

   private fun createSphinxWebViewDto(data: String) {
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

    fun authorizeWebApp(amount: Int) {
        if (amount > 0) {
            if (!challenge.isNullOrEmpty()) {
                // Sign challenge
            } else {
                contactRepository.accountOwner.value?.nodePubKey?.let {
                    val sendAuth = SendAuth(
                        budget = amount.toString(),
                        pubkey = it.value,
                        type = "AUTHORIZE",
                        password = generateRandomPass(),
                        application = "Sphinx"
                    ).generateSendAuthString()

                    webViewViewStateContainer.updateViewState(
                        WebViewViewState.SendAuthorization(
                            sendAuth
                        )
                    )
                }
            }
        }
    }

    private fun handleWebAppJson() {
        viewModelScope.launch(mainImmediate) {
            sphinxWebViewDtoStateFlow.collect {
                when (it?.type) {
                    "AUTHORIZE" -> {
                        webViewViewStateContainer.updateViewState(WebViewViewState.Authorization)
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