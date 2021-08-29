package chat.sphinx.subscription.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.subscription.R
import chat.sphinx.subscription.databinding.FragmentSubscriptionBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
internal class SubscriptionFragment: BaseFragment<
        SubscriptionViewState,
        SubscriptionViewModel,
        FragmentSubscriptionBinding
        >(R.layout.fragment_subscription)
{
    override val viewModel: SubscriptionViewModel by viewModels()
    override val binding: FragmentSubscriptionBinding by viewBinding(FragmentSubscriptionBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeSubscriptionHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.subscription_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch { viewModel.navigator.closeDetailScreen() }
            }
            textViewDetailScreenHeaderNavBack.apply {
                visible
                setOnClickListener {
                    lifecycleScope.launch { viewModel.navigator.popBackStack() }
                }
            }
        }

        binding.apply {
            val calendar = Calendar.getInstance()

            editTextPayUntil.setOnClickListener {
                this@SubscriptionFragment.context?.let { context ->
                    val datePickerDialog = DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            calendar.set(Calendar.YEAR, year)
                            calendar.set(Calendar.MONTH, month)
                            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                            editTextPayUntil.setText(
                                SimpleDateFormat("dd/MM/yyyy", Locale.US).format(calendar.time)
                            )
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH),
                    )
                    datePickerDialog.datePicker.minDate = calendar.timeInMillis
                    datePickerDialog.show()
                }

            }

            radioGroupAmount.setOnCheckedChangeListenerWithInputInteraction(
                radioButtonCustomAmount, editTextCustomAmount
            )

            radioGroupEndRule.setOnCheckedChangeListenerWithInputInteraction(
                radioButtonMake, editTextMakeQuantity
            )

            radioGroupEndRule.setOnCheckedChangeListenerWithDatePickerInputInteraction(
                radioButtonUntil, editTextPayUntil
            )

            buttonSave.setOnClickListener {

                val amount: Int? = when (radioGroupAmount.checkedRadioButtonId) {
                    R.id.radio_button_500_sats -> {
                        500
                    }
                    R.id.radio_button_1000_sats -> {
                        1000
                    }
                    R.id.radio_button_2000_sats -> {
                        2000
                    }
                    R.id.radio_button_custom_amount -> {
                        editTextCustomAmount.text?.toString()?.toIntOrNull()
                    }
                    else -> null
                }

                val cron: String? = when (radioGroupTimeInterval.checkedRadioButtonId) {
                    R.id.radio_button_daily -> {
                        "daily"
                    }
                    R.id.radio_button_weekly -> {
                        "weekly"
                    }
                    R.id.radio_button_monthly -> {
                        "monthly"
                    }
                    else -> null
                }

                val endDate: Date? = when (radioGroupEndRule.checkedRadioButtonId) {
                    R.id.radio_button_make -> {
                        // TODO: Calculate depending on the timeInterval
                        Calendar.getInstance().time
                    }
                    R.id.radio_button_until -> {
                        // TODO: Load editTextPayUntil.text into date
                        Calendar.getInstance().time
                    }
                    else -> null
                }

                viewModel.saveSubscription(
                    amount,
                    cron,
                    endDate
                )
            }
            (requireActivity() as InsetterActivity).addNavigationBarPadding(layoutConstraintSubscription)
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: SubscriptionViewState) {
//        TODO("Not yet implemented")
    }
}
