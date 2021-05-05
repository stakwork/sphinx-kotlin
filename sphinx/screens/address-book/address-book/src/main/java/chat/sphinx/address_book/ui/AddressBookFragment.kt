package chat.sphinx.address_book.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.address_book.R
import chat.sphinx.address_book.ui.adapter.AddressBookListAdapter
import chat.sphinx.address_book.databinding.FragmentAddressBookBinding
import chat.sphinx.address_book.navigation.AddressBookNavigator
import chat.sphinx.address_book.ui.adapter.AddressBookFooterAdapter
import chat.sphinx.address_book.ui.adapter.SwipeToDeleteCallback
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addStatusBarPadding
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

        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutAddressBookHeader.layoutConstraintAddressBookHeader)

        setupContacts()
    }

    private fun setupContacts() {
        val addressBookListAdapter = AddressBookListAdapter(imageLoader, viewLifecycleOwner, viewModel)
        val addressBookFooterAdapter = AddressBookFooterAdapter(requireActivity() as InsetterActivity)
        binding.recyclerViewContacts.apply {
            this.setHasFixedSize(false)
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = ConcatAdapter(addressBookListAdapter, addressBookFooterAdapter)
        }

        context?.let {
            val swipeHandler = object : SwipeToDeleteCallback(it) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    addressBookListAdapter.removeAt(viewHolder.bindingAdapterPosition)
                }
            }
            val itemTouchHelper = ItemTouchHelper(swipeHandler)
            itemTouchHelper.attachToRecyclerView(binding.recyclerViewContacts)
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: AddressBookViewState) {
//        TODO("Not yet implemented")
    }
}
