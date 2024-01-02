package chat.sphinx.new_contact.ui

import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.contact.databinding.LayoutContactBinding
import chat.sphinx.contact.databinding.LayoutContactDetailScreenHeaderBinding
import chat.sphinx.contact.databinding.LayoutContactSaveBinding
import chat.sphinx.contact.ui.ContactFragment
import chat.sphinx.new_contact.R
import chat.sphinx.new_contact.databinding.FragmentNewContactBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class NewContactFragment: ContactFragment<
        FragmentNewContactBinding,
        NewContactFragmentArgs,
        NewContactViewModel
        >(R.layout.fragment_new_contact)
{

    @Inject
    lateinit var userColorsHelperInj: UserColorsHelper

    override val userColorsHelper: UserColorsHelper
        get() = userColorsHelperInj

    override val viewModel: NewContactViewModel by viewModels()
    override val binding: FragmentNewContactBinding by viewBinding(FragmentNewContactBinding::bind)

    override val headerBinding: LayoutContactDetailScreenHeaderBinding by viewBinding(
        LayoutContactDetailScreenHeaderBinding::bind, R.id.include_new_contact_header
    )

    override val contactBinding: LayoutContactBinding by viewBinding(
        LayoutContactBinding::bind, R.id.include_new_contact_layout
    )

    override val contactSaveBinding: LayoutContactSaveBinding by viewBinding(
        LayoutContactSaveBinding::bind, R.id.include_new_contact_layout_save
    )

    override fun getHeaderText(): String = getString(R.string.new_contact_header_name)

    override fun getSaveButtonText(): String = getString(R.string.save_to_contacts_button)

}
