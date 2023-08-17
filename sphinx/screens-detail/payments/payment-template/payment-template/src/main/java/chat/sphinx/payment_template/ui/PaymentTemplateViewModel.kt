package chat.sphinx.payment_template.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.SendPayment
import chat.sphinx.kotlin_response.Response
import chat.sphinx.payment_template.R
import chat.sphinx.payment_template.navigation.PaymentTemplateNavigator
import chat.sphinx.payment_template.ui.viewstate.PaymentTemplateViewState
import chat.sphinx.payment_template.ui.viewstate.SelectedTemplateViewState
import chat.sphinx.payment_template.ui.viewstate.TemplateImagesViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.payment.PaymentTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val PaymentTemplateFragmentArgs.chatId: ChatId?
    get() = if (argChatId == ChatId.NULL_CHAT_ID.toLong()) {
        null
    } else {
        ChatId(argChatId)
    }

internal inline val PaymentTemplateFragmentArgs.contactId: ContactId
    get() = ContactId(argContactId)

internal inline val PaymentTemplateFragmentArgs.amount: Sat
    get() = Sat(argAmount)

internal inline val PaymentTemplateFragmentArgs.message: String?
    get() = if (argMessage.isEmpty()) {
        null
    } else {
        argMessage
    }

@HiltViewModel
internal class PaymentTemplateViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val app: Application,
    private val messageRepository: MessageRepository,
    val navigator: PaymentTemplateNavigator
): SideEffectViewModel<
        Context,
        PaymentTemplateSideEffect,
        PaymentTemplateViewState>(dispatchers, PaymentTemplateViewState.Idle)
{

    private val sendPaymentBuilder = SendPayment.Builder()

    private val args: PaymentTemplateFragmentArgs by savedStateHandle.navArgs()

    val templateImagesViewStateContainer: ViewStateContainer<TemplateImagesViewState> by lazy {
        ViewStateContainer(TemplateImagesViewState.LoadingTemplateImages)
    }

    val selectedTemplateViewStateContainer: ViewStateContainer<SelectedTemplateViewState> by lazy {
        ViewStateContainer(SelectedTemplateViewState.Idle)
    }

    private var loadTemplateImagesJob: Job? = null
    fun loadTemplateImages() {
        if (loadTemplateImagesJob?.isActive == true) {
            return
        }

        loadTemplateImagesJob = viewModelScope.launch(mainImmediate) {
            templateImagesViewStateContainer.updateViewState(TemplateImagesViewState.LoadingTemplateImages)

            when (val response = messageRepository.getPaymentTemplates()) {
                is Response.Error -> {
                    templateImagesViewStateContainer.updateViewState(
                        TemplateImagesViewState.TemplateImages(listOf())
                    )
                }
                is Response.Success -> {
                    templateImagesViewStateContainer.updateViewState(
                        TemplateImagesViewState.TemplateImages(response.value)
                    )
                }
            }
        }
    }

    fun selectTemplate(position: Int) {
        val templateImagesViewState = templateImagesViewStateContainer.value

        if (templateImagesViewState is TemplateImagesViewState.TemplateImages) {

            templateImagesViewState.templates.getOrNull(position)?.let { template ->
                sendPaymentBuilder.setPaymentTemplate(template)

                selectedTemplateViewStateContainer.updateViewState(
                    SelectedTemplateViewState.SelectedTemplate(template)
                )
            } ?: run {
                sendPaymentBuilder.setPaymentTemplate(null)

                selectedTemplateViewStateContainer.updateViewState(
                    SelectedTemplateViewState.Idle
                )
            }
        }
    }

    private var sendPaymentJob: Job? = null
    fun sendPayment() {
        if (sendPaymentJob?.isActive == true) {
            return
        }

        sendPaymentBuilder.setChatId(args.chatId)
        sendPaymentBuilder.setContactId(args.contactId)
        sendPaymentBuilder.setAmount(args.amount.value)
        sendPaymentBuilder.setText(args.message)

        viewStateContainer.updateViewState(PaymentTemplateViewState.ProcessingPayment)

        sendPaymentJob = viewModelScope.launch(mainImmediate) {
            val sendPayment = sendPaymentBuilder.build()
            when (val response = messageRepository.sendPayment(sendPayment)) {
                is Response.Error -> {
                    submitSideEffect(
                        PaymentTemplateSideEffect.Notify(
                            String.format(app.getString(R.string.error_payment_message), response.cause.message))
                    )
                    viewStateContainer.updateViewState(PaymentTemplateViewState.PaymentFailed)
                }
                is Response.Success -> {
                    navigator.closeDetailScreen()
                }
            }
        }
    }
}