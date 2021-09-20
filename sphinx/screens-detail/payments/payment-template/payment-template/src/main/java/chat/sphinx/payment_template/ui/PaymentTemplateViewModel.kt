package chat.sphinx.payment_template.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.SendPayment
import chat.sphinx.kotlin_response.Response
import chat.sphinx.payment_common.ui.PaymentSideEffect
import chat.sphinx.payment_common.ui.viewstate.send.PaymentSendViewState
import chat.sphinx.payment_template.R
import chat.sphinx.payment_template.navigation.PaymentTemplateNavigator
import chat.sphinx.payment_template.ui.viewstate.PaymentTemplateViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
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

    fun sendPayment() {
        sendPaymentBuilder.setChatId(args.chatId)
        sendPaymentBuilder.setContactId(args.contactId)
        sendPaymentBuilder.setAmount(args.amount.value)
        sendPaymentBuilder.setText(args.message)

        viewStateContainer.updateViewState(PaymentTemplateViewState.ProcessingPayment)

        viewModelScope.launch(mainImmediate) {
            val sendPayment = sendPaymentBuilder.build()

            when (messageRepository.sendPayment(sendPayment)) {
                is Response.Error -> {
                    submitSideEffect(
                        PaymentTemplateSideEffect.Notify(app.getString(R.string.error_processing_payment))
                    )
                    viewStateContainer.updateViewState(PaymentTemplateViewState.PaymentFailed)
                }
                is Response.Success -> {
//                    val successMessage = app.getString(
//                        R.string.payment_sent,
//                        sendPayment?.amount ?: 0,
//                        sendPayment?.destinationKey?.value ?: "Unknown"
//                    )
//
//                    submitSideEffect(
//                        PaymentSideEffect.Notify(successMessage)
//                    )

                    navigator.closeDetailScreen()
                }
            }
        }
    }
}