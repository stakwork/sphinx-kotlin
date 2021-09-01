package chat.sphinx.subscription.ui

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.subscription.R
import chat.sphinx.subscription.databinding.FragmentSubscriptionBinding
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.toDateTime
import chat.sphinx.wrapper_common.toDateTimeWithFormat
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
internal class SubscriptionFragment: SideEffectFragment<
        Context,
        SubscriptionSideEffect,
        SubscriptionViewState,
        SubscriptionViewModel,
        FragmentSubscriptionBinding
        >(R.layout.fragment_subscription)
{
    override val viewModel: SubscriptionViewModel by viewModels()
    override val binding: FragmentSubscriptionBinding by viewBinding(FragmentSubscriptionBinding::bind)

    private val calendar = Calendar.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendar.timeZone = TimeZone.getTimeZone(DateTime.UTC)

        binding.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.subscription_header_name)

            textViewDetailScreenHeaderNavBack.apply {
                visible
                setOnClickListener {
                    lifecycleScope.launch { viewModel.navigator.popBackStack() }
                }
            }

            textViewDetailSubscriptionDelete.setOnClickListener {
                viewModel.deleteSubscription()
            }

            switchSubscriptionEnablement.setOnClickListener {
                // Toggle checked status
                switchSubscriptionEnablement.isChecked = !switchSubscriptionEnablement.isChecked

                if (switchSubscriptionEnablement.isChecked) {
                    // We are about to pause the subscription ask for confirmation
                    viewModel.pauseSubscription()
                } else {
                    // We should restart the subscription
                    viewModel.restartSubscription()
                }
            }

            editTextSubscriptionPayUntil.setOnClickListener {
                val datePickerDialog = DatePickerDialog(
                    root.context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(Calendar.YEAR, year)
                        calendar.set(Calendar.MONTH, month)
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                        editTextSubscriptionPayUntil.setText(
                            DateTime.getFormatMMMddyyyy(
                                TimeZone.getTimeZone(DateTime.UTC)
                            ).format(calendar.time)
                        )
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                )
                datePickerDialog.datePicker.minDate = Date().time
                datePickerDialog.show()
            }

            radioGroupSubscriptionAmount.setOnCheckedChangeListenerWithInputInteraction(
                radioButtonSubscriptionAmountCustom, editTextSubscriptionCustomAmount
            )

            radioGroupSubscriptionEndRule.setOnCheckedChangeListenerWithInputInteraction(
                radioButtonSubscriptionMakeQuantity, editTextSubscriptionMakeQuantity
            )

            radioGroupSubscriptionEndRule.setOnCheckedChangeListenerWithDatePickerInputInteraction(
                radioButtonSubscriptionPayUntil, editTextSubscriptionPayUntil
            )

            buttonSubscriptionSave.setOnClickListener {

                val amount: Sat? = when (radioGroupSubscriptionAmount.checkedRadioButtonId) {
                    R.id.radio_button_subscription_amount_500_sats -> {
                        Sat(500)
                    }
                    R.id.radio_button_subscription_amount_1000_sats -> {
                        Sat(1000)
                    }
                    R.id.radio_button_subscription_amount_2000_sats -> {
                        Sat(2000)
                    }
                    R.id.radio_button_subscription_amount_custom -> {
                        editTextSubscriptionCustomAmount.text?.toString()?.toLongOrNull()?.let {
                            Sat(it)
                        }
                    }
                    else -> null
                }

                val cron: String? = when (radioGroupSubscriptionTimeInterval.checkedRadioButtonId) {
                    R.id.radio_button_subscription_daily -> {
                        "daily"
                    }
                    R.id.radio_button_subscription_weekly -> {
                        "weekly"
                    }
                    R.id.radio_button_subscription_monthly -> {
                        "monthly"
                    }
                    else -> null
                }

                var endNumber: Long? = null
                val endDate: DateTime? = when (radioGroupSubscriptionEndRule.checkedRadioButtonId) {
                    R.id.radio_button_subscription_make_quantity -> {
                        editTextSubscriptionMakeQuantity.text?.toString()?.let {
                            endNumber = it.toLongOrNull()
                        }
                        null
                    }
                    R.id.radio_button_subscription_pay_until -> {
                        calendar.timeInMillis.toDateTime()
                    }
                    else -> null
                }

                viewModel.saveSubscription(
                    amount,
                    cron,
                    endDate,
                    endNumber
                )
            }
            (requireActivity() as InsetterActivity).addNavigationBarPadding(layoutConstraintSubscription)
        }

        viewModel.initSubscription()
    }

    override suspend fun onViewStateFlowCollect(viewState: SubscriptionViewState) {
        when(viewState) {
            is SubscriptionViewState.Idle -> {
                // Setup for new subscription
                binding.apply {
                    progressBarSubscriptionSave.gone
                    textViewDetailSubscriptionDelete.gone
                    layoutConstraintSubscriptionEnablement.gone
                    buttonSubscriptionSave.text = getString(R.string.subscribe)
                }
            }
            is SubscriptionViewState.SubscriptionLoaded -> {
                binding.apply {
                    progressBarSubscriptionSave.gone
                    textViewDetailSubscriptionDelete.visible
                    layoutConstraintSubscriptionEnablement.visible
                    buttonSubscriptionSave.text = getString(R.string.update_subscription)

                    switchSubscriptionEnablement.isChecked = !viewState.subscription.ended && !viewState.subscription.paused

                    // Populate Amount
                    when (viewState.subscription.amount.value) {
                        500L -> {
                            radioButtonSubscriptionAmount500Sats.isChecked = true
                        }
                        1000L -> {
                            radioButtonSubscriptionAmount1000Sats.isChecked = true
                        }
                        2000L -> {
                            radioButtonSubscriptionAmount2000Sats.isChecked = true
                        }
                        else -> {
                            radioButtonSubscriptionAmountCustom.isChecked = true

                            editTextSubscriptionCustomAmount.setText(
                                viewState.subscription.amount.value.toString()
                            )
                        }
                    }

                    // Populate Time Interval
                    when  {
                        viewState.subscription.cron.value.endsWith("* * *") -> {
                            // Daily...
                            radioButtonSubscriptionDaily.isChecked = true
                        }
                        viewState.subscription.cron.value.endsWith("* *") -> {
                            // Monthly
                            radioButtonSubscriptionMonthly.isChecked = true
                        }
                        else -> {
                            // Weekly
                            radioButtonSubscriptionWeekly.isChecked = true
                        }
                    }

                    // Populate End Rule
                    when {
                        viewState.subscription.end_number != null -> {
                            radioButtonSubscriptionMakeQuantity.isChecked = true

                            editTextSubscriptionMakeQuantity.setText(
                                viewState.subscription.end_number!!.value.toString()
                            )
                        }
                        viewState.subscription.end_date != null -> {
                            radioButtonSubscriptionPayUntil.isChecked = true

                            editTextSubscriptionPayUntil.setText(
                                DateTime.getFormatMMMddyyyy(
                                    TimeZone.getTimeZone(DateTime.UTC)
                                ).format(viewState.subscription.end_date!!.value)
                            )

                            calendar.time = viewState.subscription.end_date!!.value
                        }
                    }
                }
            }
            is SubscriptionViewState.SavingSubscription -> {
                binding.progressBarSubscriptionSave.visible
            }
            is SubscriptionViewState.SavingSubscriptionFailed -> {
                binding.progressBarSubscriptionSave.gone
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: SubscriptionSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
