package chat.sphinx.subscription.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_subscription.SubscriptionRepository
import chat.sphinx.kotlin_response.Response
import chat.sphinx.subscription.R
import chat.sphinx.subscription.navigation.SubscriptionNavigator
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.subscription.EndNumber
import chat.sphinx.wrapper_subscription.Subscription
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
internal class SubscriptionViewModel @Inject constructor(
    val app: Application,
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val subscriptionRepository: SubscriptionRepository,
    val navigator: SubscriptionNavigator
): SideEffectViewModel<
        Context,
        SubscriptionSideEffect,
        SubscriptionViewState
        >(dispatchers, SubscriptionViewState.Idle)
{
    private val args: SubscriptionFragmentArgs by savedStateHandle.navArgs()

    fun initSubscription() {
        viewModelScope.launch(mainImmediate) {
            subscriptionRepository.getActiveSubscriptionByContactId(ContactId(args.argContactId)).firstOrNull().let { subscription ->
                if (subscription == null) {
                    updateViewState(
                        SubscriptionViewState.Idle
                    )
                } else {
                    updateViewState(
                        SubscriptionViewState.SubscriptionLoaded(
                            subscription
                        )
                    )
                }
            }
        }

    }

    fun saveSubscription(
        amount: Sat?,
        interval: String?,
        endDate: DateTime?,
        endNumber: Long?
    ) {
        viewModelScope.launch(mainImmediate) {

            if (amount == null) {
                submitSideEffect(
                    SubscriptionSideEffect.Notify(
                        app.getString(R.string.amount_is_required)
                    )
                )
                return@launch
            }

            if (interval == null) {
                submitSideEffect(
                    SubscriptionSideEffect.Notify(
                        app.getString(R.string.time_interval_is_required)
                    )
                )
                return@launch
            }

            if (endNumber == null && endDate == null) {
                submitSideEffect(
                    SubscriptionSideEffect.Notify(
                        app.getString(R.string.please_set_either_the_number_of_payments_to_make_or_end_date)
                    )
                )
                return@launch
            }

            subscriptionRepository.getActiveSubscriptionByContactId(
                ContactId(args.argContactId)
            ).firstOrNull().let { subscription ->
                val loadResponse = if (subscription == null) {
                    subscriptionRepository.createSubscription(
                        amount = amount,
                        interval = interval,
                        contactId = ContactId(args.argContactId),
                        chatId = null,
                        endDate = endDate?.let { DateTime.getFormatMMMddyyyy().format(it.value) },
                        endNumber = endNumber?.let { EndNumber(it) }
                    )
                } else {
                    subscriptionRepository.updateSubscription(
                        id = subscription.id,
                        amount = amount,
                        interval = interval,
                        contactId = ContactId(args.argContactId),
                        chatId = subscription.chat_id,
                        endDate = endDate?.let { DateTime.getFormatMMMddyyyy().format(it.value) },
                        endNumber = endNumber?.let { EndNumber(it) }
                    )
                }

                when (loadResponse) {
                    is Response.Error -> {
                        submitSideEffect(
                            SubscriptionSideEffect.Notify(app.getString(R.string.failed_to_save_subscription))
                        )
                    }
                    is Response.Success -> {
                        updateViewState(
                            SubscriptionViewState.CloseSubscriptionDetail
                        )
                    }
                }
            }
        }
    }

    fun deleteSubscription() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(
                SubscriptionSideEffect.AlertConfirmDeleteSubscription() {
                    viewModelScope.launch(mainImmediate) {
                        subscriptionRepository.getActiveSubscriptionByContactId(ContactId(args.argContactId)).firstOrNull().let { subscription ->
                            if (subscription == null) {
                                updateViewState(
                                    SubscriptionViewState.CloseSubscriptionDetail
                                )
                            } else {
                                when(subscriptionRepository.deleteSubscription(subscription.id)) {
                                    is Response.Error -> {
                                        submitSideEffect(
                                            SubscriptionSideEffect.Notify(app.getString(R.string.failed_to_delete_subscription))
                                        )
                                    }
                                    is Response.Success -> {
                                        updateViewState(
                                            SubscriptionViewState.CloseSubscriptionDetail
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    fun pauseSubscription() {
        viewModelScope.launch(mainImmediate) {
            subscriptionRepository.getActiveSubscriptionByContactId(ContactId(args.argContactId)).firstOrNull().let { subscription ->
                if (subscription == null) {
                    submitSideEffect(
                        SubscriptionSideEffect.Notify(app.getString(R.string.failed_to_pause_subscription))
                    )
                } else {
                    when (subscriptionRepository.pauseSubscription(subscription.id)) {
                        is Response.Error -> {
                            submitSideEffect(
                                SubscriptionSideEffect.Notify(app.getString(R.string.failed_to_pause_subscription))
                            )
                        }
                        is Response.Success -> {
                            submitSideEffect(
                                SubscriptionSideEffect.Notify(app.getString(R.string.successfully_paused_subscription))
                            )
                            updateViewState(
                                SubscriptionViewState.SubscriptionLoaded(
                                    Subscription(
                                        id = subscription.id,
                                        subscription.cron,
                                        subscription.amount,
                                        subscription.end_number,
                                        subscription.count,
                                        subscription.end_date,
                                        subscription.ended,
                                        paused = true,
                                        subscription.created_at,
                                        subscription.updated_at,
                                        subscription.chat_id,
                                        subscription.contact_id
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun restartSubscription() {
        viewModelScope.launch(mainImmediate) {
            subscriptionRepository.getActiveSubscriptionByContactId(ContactId(args.argContactId)).firstOrNull().let { subscription ->
                if (subscription == null) {
                    submitSideEffect(
                        SubscriptionSideEffect.Notify(app.getString(R.string.failed_to_restart_subscription))
                    )
                } else {
                    when (subscriptionRepository.restartSubscription(subscription.id)) {
                        is Response.Error -> {
                            submitSideEffect(
                                SubscriptionSideEffect.Notify(app.getString(R.string.failed_to_restart_subscription))
                            )
                        }
                        is Response.Success -> {
                            updateViewState(
                                SubscriptionViewState.SubscriptionLoaded(
                                    Subscription(
                                        id = subscription.id,
                                        subscription.cron,
                                        subscription.amount,
                                        subscription.end_number,
                                        subscription.count,
                                        subscription.end_date,
                                        subscription.ended,
                                        paused = false,
                                        subscription.created_at,
                                        subscription.updated_at,
                                        subscription.chat_id,
                                        subscription.contact_id
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
