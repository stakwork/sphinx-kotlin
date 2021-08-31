package chat.sphinx.subscription.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_subscription.SubscriptionRepository
import chat.sphinx.kotlin_response.Response
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
        cron: String?,
        endDate: Date?,
        endNumber: Long?
    ) {
        viewModelScope.launch(mainImmediate) {

            if (amount == null) {
                submitSideEffect(
                    SubscriptionSideEffect.Notify(
                        "Amount is required"
                    )
                )
                return@launch
            }

            if (cron == null) {
                submitSideEffect(
                    SubscriptionSideEffect.Notify(
                        "Time Interval is required"
                    )
                )
                return@launch
            }

            if (endNumber == null && endDate == null) {
                submitSideEffect(
                    SubscriptionSideEffect.Notify(
                        "Please set either the number of payments to make or end date"
                    )
                )
                return@launch
            }

            subscriptionRepository.getSubscriptionByContactId(ContactId(args.argContactId)).firstOrNull().let { subscription ->
                val loadResponse = if (subscription == null) {
                    subscriptionRepository.createSubscription(
                        amount = amount,
                        interval = "daily",
                        contactId = ContactId(args.argContactId),
                        chatId = null,
                        endDate = null, // TODO: Fix this
                        endNumber = endNumber?.let { EndNumber(it) }
                    )
                } else {
                    subscriptionRepository.updateSubscription(
                        Subscription(
                            id = subscription.id,
                            cron = Cron(cron),
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
                            SubscriptionSideEffect.Notify("Failed to save subscription")
                        )
                    }
                    is Response.Success -> {
                        submitSideEffect(
                            SubscriptionSideEffect.Notify("Saved subscription successfully")
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
                            SubscriptionSideEffect.Notify("Deleting subscription")
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
                        SubscriptionSideEffect.Notify("Failed to pause subscription")
                    )
                } else {
                    when (subscriptionRepository.pauseSubscription(subscription.id)) {
                        is Response.Error -> {
                            submitSideEffect(
                                SubscriptionSideEffect.Notify("Failed to pause subscription")
                            )
                        }
                        is Response.Success -> {
                            submitSideEffect(
                                SubscriptionSideEffect.Notify("Successfully paused subscription")
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
                        SubscriptionSideEffect.Notify("Failed to restart subscription")
                    )
                } else {
                    when (subscriptionRepository.pauseSubscription(subscription.id)) {
                        is Response.Error -> {
                            submitSideEffect(
                                SubscriptionSideEffect.Notify("Failed to restart subscription")
                            )
                        }
                        is Response.Success -> {
                            submitSideEffect(
                                SubscriptionSideEffect.Notify("Successfully restarted subscription")
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
