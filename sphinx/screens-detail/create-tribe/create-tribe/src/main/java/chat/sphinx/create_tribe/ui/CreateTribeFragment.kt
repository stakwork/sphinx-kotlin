package chat.sphinx.create_tribe.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_repository_chat.model.CreateTribe
import chat.sphinx.create_tribe.R
import chat.sphinx.create_tribe.databinding.FragmentCreateTribeBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class CreateTribeFragment: BaseFragment<
        CreateTribeViewState,
        CreateTribeViewModel,
        FragmentCreateTribeBinding
        >(R.layout.fragment_create_tribe)
{
    override val viewModel: CreateTribeViewModel by viewModels()
    override val binding: FragmentCreateTribeBinding by viewBinding(FragmentCreateTribeBinding::bind)

    private val createTribeBuilder = CreateTribe.Builder()
    private val MILLISECONDS_IN_AN_HOUR = 3_600_000L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeCreateTribeHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.create_tribe_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        setupFragmentLayout()
        setupCreateTribe()
    }

    fun setupFragmentLayout() {
        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.constraintLayoutCreateTribeFragment)
    }

    fun setupCreateTribe() {
        binding.apply {
            editTextTribeName.addTextChangedListener {
                createTribeBuilder.setName(it.toString())

                updateCreateButtonState()
            }
            // TODO: Add Image Functionality...
            editTextTribeDescription.addTextChangedListener {
                createTribeBuilder.setDescription(it.toString())

                updateCreateButtonState()
            }
            // TODO: Add tags functionality
            editTextTribePriceToJoin.addTextChangedListener {
                createTribeBuilder.setPriceToJoin(
                    if (it.isNullOrEmpty()) {
                        null
                    } else {
                        it.toString().toLong()
                    }
                )
            }
            editTextTribePricePerMessage.addTextChangedListener {
                createTribeBuilder.setPricePerMessage(
                    if (it.isNullOrEmpty()) {
                        null
                    } else {
                        it.toString().toLong()
                    }
                )
            }
            editTextTribeAmountToStake.addTextChangedListener {

                createTribeBuilder.setEscrowAmount(
                    if (it.isNullOrEmpty()) {
                        null
                    } else {
                        it.toString().toLong()
                    }
                )
            }
            editTextTribeTimeToStake.addTextChangedListener {
                createTribeBuilder.setEscrowMillis(
                    if (it.isNullOrEmpty()) {
                        null
                    } else {
                        it.toString().toLong() * MILLISECONDS_IN_AN_HOUR
                    }
                )
            }
            editTextTribeAppUrl.addTextChangedListener {
                createTribeBuilder.setAppUrl(it.toString())
            }
            editTextTribeFeedUrl.addTextChangedListener {
                createTribeBuilder.setFeedUrl(it.toString())
            }
            // TODO: add listing functionality
            // TODO: add approval functionality

            includeCreateTribeButton.layoutConstraintButtonCreateTribe.setOnClickListener {
                if (createTribeBuilder.hasRequiredFields) {
                    // TODO: createTribe
                } else {
                    // TODO: Give  user feed back that they need name+description
                }
            }
        }
    }

    private fun updateCreateButtonState() {
        requireActivity().let {
            binding.includeCreateTribeButton.apply {
                if (layoutConstraintButtonCreateTribe.isEnabled != createTribeBuilder.hasRequiredFields) {
                    layoutConstraintButtonCreateTribe.isEnabled = createTribeBuilder.hasRequiredFields
                    layoutConstraintButtonCreateTribe.background = if (createTribeBuilder.hasRequiredFields) {
                        AppCompatResources.getDrawable(it, R.drawable.button_background_enabled)
                    } else {
                        AppCompatResources.getDrawable(it, R.drawable.button_background_disabled)
                    }
                }
            }
        }
    }
    override suspend fun onViewStateFlowCollect(viewState: CreateTribeViewState) {
//        TODO("Not yet implemented")
    }
}
