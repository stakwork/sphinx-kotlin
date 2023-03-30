package chat.sphinx.edit_contact.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.contact.databinding.LayoutContactBinding
import chat.sphinx.contact.databinding.LayoutContactDetailScreenHeaderBinding
import chat.sphinx.contact.databinding.LayoutContactSaveBinding
import chat.sphinx.contact.ui.ContactFragment
import chat.sphinx.edit_contact.R
import chat.sphinx.edit_contact.databinding.FragmentEditContactBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class EditContactFragment : ContactFragment<
        FragmentEditContactBinding,
        EditContactFragmentArgs,
        EditContactViewModel
        >(R.layout.fragment_edit_contact)
{
    @Inject
    lateinit var userColorsHelperInj: UserColorsHelper

    override val userColorsHelper: UserColorsHelper
        get() = userColorsHelperInj

    override val viewModel: EditContactViewModel by viewModels()
    override val binding: FragmentEditContactBinding by viewBinding(FragmentEditContactBinding::bind)

    override val headerBinding: LayoutContactDetailScreenHeaderBinding by viewBinding(
        LayoutContactDetailScreenHeaderBinding::bind, R.id.include_edit_contact_header
    )
    override val contactBinding: LayoutContactBinding by viewBinding(
        LayoutContactBinding::bind, R.id.include_edit_contact_layout
    )

    override val contactSaveBinding: LayoutContactSaveBinding by viewBinding(
        LayoutContactSaveBinding::bind, R.id.include_edit_contact_layout_save
    )

    override fun getHeaderText(): String = getString(R.string.edit_contact_header_name)

    override fun getSaveButtonText(): String = getString(R.string.save_contact_button)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        headerBinding.apply {
            textViewDetailScreenSubscribe.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.toSubscriptionDetailScreen()
                }
            }
        }
    }
}
