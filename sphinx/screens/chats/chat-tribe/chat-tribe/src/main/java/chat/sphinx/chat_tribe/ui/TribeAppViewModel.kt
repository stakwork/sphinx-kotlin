package chat.sphinx.chat_tribe.ui

import android.app.Application
import android.webkit.JavascriptInterface
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.model.*
import chat.sphinx.chat_tribe.model.SphinxWebViewDto.Companion.APPLICATION_NAME
import chat.sphinx.chat_tribe.model.SphinxWebViewDto.Companion.TYPE_AUTHORIZE
import chat.sphinx.chat_tribe.model.SphinxWebViewDto.Companion.TYPE_LSAT
import chat.sphinx.chat_tribe.ui.viewstate.WebViewLayoutScreenViewState
import chat.sphinx.chat_tribe.ui.viewstate.TribeFeedViewState
import chat.sphinx.chat_tribe.ui.viewstate.WebAppViewState
import chat.sphinx.chat_tribe.ui.viewstate.WebViewViewState
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.webview.LsatWebViewDto
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_chat.AppUrl
import chat.sphinx.wrapper_common.lightning.Bolt11
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toLightningPaymentRequestOrNull
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.clazzes.PasswordGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class TribeAppViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val app: Application,
    private val contactRepository: ContactRepository,
    private val moshi: Moshi,
    private val networkQueryLightning: NetworkQueryLightning
    ) : BaseViewModel<TribeFeedViewState>(dispatchers, TribeFeedViewState.Idle) {

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

    val webAppViewStateContainer: ViewStateContainer<WebAppViewState> by lazy {
        ViewStateContainer(WebAppViewState.NoApp)
    }

    private val _budgetStateFlow: MutableStateFlow<Sat> by lazy {
        MutableStateFlow(Sat(0))
    }

    val budgetStateFlow: StateFlow<Sat>
        get() = _budgetStateFlow.asStateFlow()

    init {
        handleWebAppJson()
    }

    fun init(url: TribeFeedData.Result) {
        (url as? TribeFeedData.Result.FeedData)?.appUrl?.let { url ->
            webAppViewStateContainer.updateViewState(
                WebAppViewState.AppAvailable.WebViewClosed(url)
            )
        }
    }

    fun toggleWebAppView() {
        when(webAppViewStateContainer.value) {
            is WebAppViewState.AppAvailable.WebViewClosed -> {
                (webAppViewStateContainer.value as? WebAppViewState.AppAvailable.WebViewClosed)?.let {
                    webAppViewStateContainer.updateViewState(
                        WebAppViewState.AppAvailable.WebViewOpen.Loading(it.appUrl)
                    )
                    webViewLayoutScreenViewStateContainer.updateViewState(WebViewLayoutScreenViewState.Open)
                }
            }
            is WebAppViewState.AppAvailable.WebViewOpen -> {
                (webAppViewStateContainer.value as? WebAppViewState.AppAvailable.WebViewOpen)?.let {
                    webAppViewStateContainer.updateViewState(
                        WebAppViewState.AppAvailable.WebViewClosed(it.appUrl)
                    )
                    webViewLayoutScreenViewStateContainer.updateViewState(WebViewLayoutScreenViewState.Closed)
                }
            }
            is WebAppViewState.NoApp -> {}
        }
    }

    fun didFinishLoadingWebView() {
        (webAppViewStateContainer.value as? WebAppViewState.AppAvailable.WebViewOpen.Loading)?.let {
            webAppViewStateContainer.updateViewState(
                WebAppViewState.AppAvailable.WebViewOpen.Loaded(it.appUrl)
            )
        }
    }

    private fun handleWebAppJson() {
        viewModelScope.launch(mainImmediate) {
            sphinxWebViewDtoStateFlow.collect { dto ->
                when (dto?.type) {
                    TYPE_AUTHORIZE -> {
                        _budgetStateFlow.value = Sat(0)

                        webViewViewStateContainer.updateViewState(WebViewViewState.RequestAuthorization)
                    }
                    TYPE_LSAT -> {
                        sphinxWebViewDtoStateFlow.value?.paymentRequest?.let {
                            decodePaymentRequest(it)
                        }
                    }
                }
            }
        }
    }

    fun hideAuthorizePopup() {
        webViewViewStateContainer.updateViewState(WebViewViewState.Idle)
    }

    fun authorizeWebApp(amountString: String) {
        hideAuthorizePopup()

        if (amountString.isNotEmpty()) {
            amountString.toIntOrNull()?.let { amount ->
                if (sphinxWebViewDtoStateFlow.value?.challenge?.isNullOrEmpty() == false) {
                    // Sign challenge
                } else {
                    contactRepository.accountOwner.value?.nodePubKey?.let {

                        _budgetStateFlow.value = Sat(amount.toLong())

                        val password = generatePassword()

                        val sendAuth = SendAuth(
                            budget = budgetStateFlow.value.value.toInt(),
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
    }

    private fun decodePaymentRequest(paymentRequest: String) {
        paymentRequest.toLightningPaymentRequestOrNull()?.let { lightningPaymentRequest ->
            try {
                val bolt11 = Bolt11.decode(lightningPaymentRequest)
                val amount = bolt11.getSatsAmount()

                amount?.let { nnAmount ->
                    if (budgetStateFlow.value.value >= (nnAmount.value)) {
                        viewModelScope.launch(mainImmediate) {
                            networkQueryLightning.payLsat(
                                LsatWebViewDto(
                                    sphinxWebViewDtoStateFlow.value?.paymentRequest,
                                    sphinxWebViewDtoStateFlow.value?.macaroon,
                                    sphinxWebViewDtoStateFlow.value?.issuer
                                )
                            ).collect { loadResponse ->
                                @Exhaustive
                                when (loadResponse) {
                                    is LoadResponse.Loading -> {}
                                    is Response.Error -> {
                                        sendLSatMessage(
                                            success = 0,
                                            lsat = null,
                                            error = app.getString(R.string.side_effect_error_pay_lsat)
                                        )
                                    }
                                    is Response.Success -> {
                                        _budgetStateFlow.value = Sat(budgetStateFlow.value.value - nnAmount.value)

                                        sendLSatMessage(
                                            success = 1,
                                            lsat = loadResponse.value.lsat,
                                            error = null
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        sendLSatMessage(
                            success = 0,
                            lsat = null,
                            error = app.getString(R.string.side_effect_insufficient_budget)
                        )
                    }
                }

            } catch (e: Exception) {
                sendLSatMessage(
                    success = 0,
                    lsat = null,
                    error = app.getString(R.string.side_effect_error_pay_lsat)
                )
            }
        }
    }

    private fun sendLSatMessage(
        success: Int,
        lsat: String? = null,
        error: String? = null
    ) {
        val password = generatePassword()

        val sendLsat = SendLsat(
            password = password,
            budget = budgetStateFlow.value.value.toString(),
            type = TYPE_LSAT,
            application = APPLICATION_NAME,
            lsat = lsat,
            success = success
        ).toJson(moshi)

        webViewViewStateContainer.updateViewState(
            WebViewViewState.SendLsat(
                "window.sphinxMessage('$sendLsat')",
                error
            )
        )
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

    private fun generatePassword(): String {
        @OptIn(RawPasswordAccess::class)
        return PasswordGenerator(passwordLength = 16).password.value.joinToString("")
    }
}