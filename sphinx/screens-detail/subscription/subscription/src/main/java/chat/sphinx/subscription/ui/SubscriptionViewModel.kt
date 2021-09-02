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
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.*
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

    companion object {
        const val DAILY_INTERVAL: String = "daily"
        const val WEEKLY_INTERVAL: String = "weekly"
        const val MONTHLY_INTERVAL: String = "MONTHLY"
    }

    private inner class SubscriptionViewStateContainer: ViewStateContainer<SubscriptionViewState>(SubscriptionViewState.Idle) {
        override val viewStateFlow: StateFlow<SubscriptionViewState> by lazy {
            flow {
                subscriptionRepository.getActiveSubscriptionByContactId(ContactId(args.argContactId)).collect { subscription ->
                    emit(
                        if (subscription != null) {
                            val timeInterval = if (subscription.cron.value.endsWith("* * *")) {
                                DAILY_INTERVAL
                            } else if (subscription.cron.value.endsWith("* *")) {
                                MONTHLY_INTERVAL
                            } else {
                                WEEKLY_INTERVAL
                            }

                            SubscriptionViewState.SubscriptionLoaded(
                                isActive = !subscription.paused,
                                amount = subscription.amount.value,
                                timeInterval = timeInterval,
                                endNumber = subscription.endNumber?.value,
                                endDate = subscription.endDate
                            )
                        } else {
                            SubscriptionViewState.Idle
                        }
                    )
                }
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                SubscriptionViewState.Idle,
            )
        }
    }

    override val viewStateContainer: ViewStateContainer<SubscriptionViewState> by lazy {
        SubscriptionViewStateContainer()
    }

    val savingSubscriptionViewStateContainer: ViewStateContainer<SavingSubscriptionViewState> by lazy {
        ViewStateContainer(SavingSubscriptionViewState.Idle)
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

            savingSubscriptionViewStateContainer.updateViewState(
                SavingSubscriptionViewState.SavingSubscription
            )

            subscriptionRepository.getActiveSubscriptionByContactId(
                ContactId(args.argContactId)
            ).firstOrNull().let { subscription ->
                val loadResponse = if (subscription == null) {
                    subscriptionRepository.createSubscription(
                        amount = amount,
                        interval = interval,
                        contactId = ContactId(args.argContactId),
                        chatId = null,
                        endDate = endDate?.let { DateTime.getFormatMMMddyyyy(TimeZone.getTimeZone("UTC")).format(it.value) },
                        endNumber = endNumber?.let { EndNumber(it) }
                    )
                } else {
                    subscriptionRepository.updateSubscription(
                        id = subscription.id,
                        amount = amount,
                        interval = interval,
                        contactId = ContactId(args.argContactId),
                        chatId = subscription.chatId,
                        endDate = endDate?.let { DateTime.getFormatMMMddyyyy(TimeZone.getTimeZone("UTC")).format(it.value) },
                        endNumber = endNumber?.let { EndNumber(it) }
                    )
                }

                when (loadResponse) {
                    is Response.Error -> {
                        submitSideEffect(
                            SubscriptionSideEffect.Notify(app.getString(R.string.failed_to_save_subscription))
                        )
                        savingSubscriptionViewStateContainer.updateViewState(
                            SavingSubscriptionViewState.SavingSubscriptionFailed
                        )
                    }
                    is Response.Success -> {
                        navigator.popBackStack()
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
                                navigator.popBackStack()
                            } else {
                                when(subscriptionRepository.deleteSubscription(subscription.id)) {
                                    is Response.Error -> {
                                        submitSideEffect(
                                            SubscriptionSideEffect.Notify(app.getString(R.string.failed_to_delete_subscription))
                                        )
                                    }
                                    is Response.Success -> {
                                        navigator.popBackStack()
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
                        is Response.Success -> {}
                    }
                }
            }
        }
    }
}
