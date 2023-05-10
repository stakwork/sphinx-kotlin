package chat.sphinx.chat_tribe.ui

import android.app.Application
import android.webkit.JavascriptInterface
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_tribe.model.*
import chat.sphinx.chat_tribe.model.SphinxWebViewDto.Companion.APPLICATION_NAME
import chat.sphinx.chat_tribe.model.SphinxWebViewDto.Companion.TYPE_AUTHORIZE
import chat.sphinx.chat_tribe.model.SphinxWebViewDto.Companion.TYPE_LSAT
import chat.sphinx.chat_tribe.ui.viewstate.WebViewLayoutScreenViewState
import chat.sphinx.chat_tribe.ui.viewstate.TribeFeedViewState
import chat.sphinx.chat_tribe.ui.viewstate.CurrentWebVieViewState
import chat.sphinx.chat_tribe.ui.viewstate.WebViewViewState
import chat.sphinx.concept_network_query_meme_server.NetworkQueryMemeServer
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.wrapper_chat.AppUrl
import chat.sphinx.wrapper_feed.FeedItemDetail
import chat.sphinx.wrapper_relay.AuthorizationToken
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.clazzes.PasswordGenerator
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
    private val contactRepository: ContactRepository,
    private val moshi: Moshi
    ) : BaseViewModel<TribeFeedViewState>(dispatchers, TribeFeedViewState.Idle) {

    @Volatile
    private var appUrl: AppUrl? = null

    private val _sphinxWebViewDtoStateFlow: MutableStateFlow<SphinxWebViewDto?> by lazy {
        MutableStateFlow(null)
    }

    private val sphinxWebViewDtoStateFlow: StateFlow<SphinxWebViewDto?>
        get() = _sphinxWebViewDtoStateFlow.asStateFlow()

    val webViewViewStateContainer: ViewStateContainer<WebViewViewState> by lazy {
        ViewStateContainer(WebViewViewState.Idle)
    }

    val webViewLayoutScreenViewStateContainer: ViewStateContainer<WebViewLayoutScreenViewState> by lazy {
        ViewStateContainer(WebViewLayoutScreenViewState.Closed)
    }

    val currentWebViewViewStateContainer: ViewStateContainer<CurrentWebVieViewState> by lazy {
        ViewStateContainer(CurrentWebVieViewState.NoWebView)
    }

    init {
        handleWebAppJson()
    }

    fun init(url: TribeFeedData.Result) {
        if (appUrl == null) {
            (url as? TribeFeedData.Result.FeedData)?.appUrl?.let { url ->
                appUrl = url

                currentWebViewViewStateContainer.updateViewState(CurrentWebVieViewState.WebViewAvailable(url))
            }
        }
    }

    fun authorizeWebApp(amount: Int) {
        if (amount > 0) {
            if (sphinxWebViewDtoStateFlow.value?.challenge?.isNullOrEmpty() == false) {
                // Sign challenge
            } else {
                @OptIn(RawPasswordAccess::class)
                val password = PasswordGenerator(passwordLength = 16).password.value.joinToString("")

                contactRepository.accountOwner.value?.nodePubKey?.let {
                    val sendAuth = SendAuth(
                        budget = amount,
                        pubkey = it.value,
                        type = TYPE_AUTHORIZE,
                        password = password,
                        application = APPLICATION_NAME
                    ).toJson(moshi)

                    webViewViewStateContainer.updateViewState(
                        WebViewViewState.SendAuthorization(
                            "window.sphinxMessage('$sendAuth')"
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
                    TYPE_AUTHORIZE -> {
                        webViewViewStateContainer.updateViewState(WebViewViewState.Authorization)
                    }
                    TYPE_LSAT -> {

                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun receiveMessage(data: String) {
        try {
            _sphinxWebViewDtoStateFlow.value =
                moshi.adapter(SphinxWebViewDto::class.java).fromJson(data)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}