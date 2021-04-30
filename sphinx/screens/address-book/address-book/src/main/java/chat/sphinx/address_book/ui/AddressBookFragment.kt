package chat.sphinx.address_book.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.address_book.R
import chat.sphinx.address_book.ui.adapter.AddressBookListAdapter
import chat.sphinx.address_book.databinding.FragmentAddressBookBinding
import chat.sphinx.address_book.navigation.AddressBookNavigator
import chat.sphinx.concept_image_loader.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class AddressBookFragment: BaseFragment<
        AddressBookViewState,
        AddressBookViewModel,
        FragmentAddressBookBinding
        >(R.layout.fragment_address_book)
{
//    companion object {
//        const val TAG = "AddressBookFragment"
//    }
//
//    @Inject
//    protected lateinit var LOG: SphinxLogger

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: AddressBookViewModel by viewModels()
    override val binding: FragmentAddressBookBinding by viewBinding(FragmentAddressBookBinding::bind)
    private val headerNavBack: TextView
        get() = binding.layoutAddressBookHeader.textViewAddressBookHeaderNavBack

    @Inject
    protected lateinit var addressBookNavigatorInj: AddressBookNavigator
    private val addressBookNavigator: AddressBookNavigator
        get() = addressBookNavigatorInj

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        LOG.d(TAG, "ON VIEW CREATED -=-=-=-=-==--=-=-=")

        headerNavBack.setOnClickListener {
            lifecycleScope.launch {
                addressBookNavigator.popBackStack()
            }
        }

        binding.layoutAddressBookButtonAddFriend.layoutConstraintAddressBookButtonAddFriend.setOnClickListener {
            lifecycleScope.launch {
                addressBookNavigator.toAddFriendDetail()
            }
        }

        setupContacts()
    }

    private fun setupContacts() {
        val addressBookListAdapter = AddressBookListAdapter(imageLoader, viewLifecycleOwner, viewModel)
        binding.layoutAddressBookContacts.recyclerViewContacts.apply {
            this.setHasFixedSize(false)
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = addressBookListAdapter
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: AddressBookViewState) {
//        TODO("Not yet implemented")
    }
}
