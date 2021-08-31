package chat.sphinx.subscription.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_subscription.SubscriptionRepository
import chat.sphinx.kotlin_response.Response
import chat.sphinx.subscription.R
import chat.sphinx.subscription.navigation.SubscriptionNavigator
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.subscription.Cron
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
    private val contactRepository: ContactRepository,
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
        updateViewState(
            SubscriptionViewState.Idle
        )
    }

    fun saveSubscription(
        amount: Sat?,
        interval: String?,
        endDate: Date?,
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

            subscriptionRepository.getSubscriptionByContactId(
                ContactId(args.argContactId)
            ).firstOrNull().let { subscription ->
                val loadResponse = if (subscription == null) {
                    subscriptionRepository.createSubscription(
                        amount = amount,
                        interval = interval,
                        contactId = ContactId(args.argContactId),
                        chatId = null,
                        endDate = null, // TODO: Fix this
                        endNumber = endNumber?.let { EndNumber(it) }
                    )
                } else {
                    subscriptionRepository.updateSubscription(
                        Subscription(
                            id = subscription.id,
                            cron = Cron(interval),
                            amount = amount,
                            end_number = subscription.end_number,
                            count = subscription.count,
                            end_date = subscription.end_date,
                            ended = subscription.ended,
                            paused = subscription.paused,
                            created_at = subscription.created_at,
                            updated_at = subscription.updated_at,
                            chat_id = subscription.chat_id,
                            contact_id = subscription.contact_id
                        )
                    )
                }

                when (loadResponse) {
                    is Response.Error -> {
                        submitSideEffect(
                            SubscriptionSideEffect.Notify(app.getString(R.string.failed_to_save_subscription))
                        )
                    }
                    is Response.Success -> {
                        submitSideEffect(
                            SubscriptionSideEffect.Notify(app.getString(R.string.saved_subscription_successfully))
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
                        submitSideEffect(
                            SubscriptionSideEffect.Notify(app.getString(R.string.deleting_subscription))
                        )
                    }
                }
            )
        }
    }

    fun pauseSubscription() {
        viewModelScope.launch(mainImmediate) {
            subscriptionRepository.getSubscriptionByContactId(ContactId(args.argContactId)).firstOrNull().let { subscription ->
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
                            // TODO: Set subscription to viewState...
//                            updateViewState(
//                                SubscriptionViewState.Subscription(
//
//                                )
//                            )
                        }
                    }
                }
            }


        }
    }

    fun restartSubscription() {
        viewModelScope.launch(mainImmediate) {
            subscriptionRepository.getSubscriptionByContactId(ContactId(args.argContactId)).firstOrNull().let { subscription ->
                if (subscription == null) {
                    submitSideEffect(
                        SubscriptionSideEffect.Notify(app.getString(R.string.failed_to_restart_subscription))
                    )
                } else {
                    when (subscriptionRepository.pauseSubscription(subscription.id)) {
                        is Response.Error -> {
                            submitSideEffect(
                                SubscriptionSideEffect.Notify(app.getString(R.string.failed_to_restart_subscription))
                            )
                        }
                        is Response.Success -> {
                            submitSideEffect(
                                SubscriptionSideEffect.Notify(app.getString(R.string.successfully_restarted_subscription))
                            )
                            // TODO: Set subscription to viewState
//                            updateViewState(
//                                SubscriptionViewState.Subscription(
//
//                                )
//                            )
                        }
                    }
                }
            }
        }
    }
}
