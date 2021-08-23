package chat.sphinx.new_contact.ui

import android.widget.ImageView
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.contact.databinding.LayoutContactBinding
import chat.sphinx.contact.ui.ContactFragment
import chat.sphinx.detail_resources.databinding.LayoutDetailScreenHeaderBinding
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

    override val viewModel: NewContactViewModel by viewModels()
    override val binding: FragmentNewContactBinding by viewBinding(FragmentNewContactBinding::bind)

    override val headerBinding: LayoutDetailScreenHeaderBinding by viewBinding(
        LayoutDetailScreenHeaderBinding::bind, R.id.include_new_contact_header
    )
    override val contactBinding: LayoutContactBinding by viewBinding(
        LayoutContactBinding::bind, R.id.include_new_contact_layout
    )

    @Inject
    lateinit var imageLoaderInj: ImageLoader<ImageView>

    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    @Inject
    lateinit var userColorsHelperInj: UserColorsHelper

    override val userColorsHelper: UserColorsHelper
        get() = userColorsHelperInj

    override fun getHeaderText(): String = getString(R.string.new_contact_header_name)

    override fun getSaveButtonText(): String = getString(R.string.save_to_contacts_button)
}
