package chat.sphinx.subscription.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.subscription.navigation.SubscriptionNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
internal class SubscriptionViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: SubscriptionNavigator
): SideEffectViewModel<
        Context,
        SubscriptionSideEffect,
        SubscriptionViewState
        >(dispatchers, SubscriptionViewState.Idle)
{

    fun saveSubscription(
        amount: Int?,
        cron: String?,
        endDate: Date?
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

            submitSideEffect(
                SubscriptionSideEffect.Notify(
                    "Subscription Saved successfully"
                )
            )
        }
    }
}
