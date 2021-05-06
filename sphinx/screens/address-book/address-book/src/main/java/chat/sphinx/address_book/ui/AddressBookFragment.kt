package chat.sphinx.address_book.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.address_book.R
import chat.sphinx.address_book.ui.adapter.AddressBookListAdapter
import chat.sphinx.address_book.databinding.FragmentAddressBookBinding
import chat.sphinx.address_book.navigation.AddressBookNavigator
import chat.sphinx.address_book.ui.adapter.AddressBookFooterAdapter
import chat.sphinx.address_book.ui.adapter.SwipeHelper
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
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

    private val header: ConstraintLayout
        get() = binding.layoutAddressBookHeader.layoutConstraintAddressBookHeader
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

        setupAddressBookHeader()

        setupContacts()
    }

    private fun setupAddressBookHeader() {
        val activity = (requireActivity() as InsetterActivity)
        activity.addStatusBarPadding(binding.layoutAddressBookHeader.layoutConstraintAddressBookHeader)

        header.layoutParams.height = header.layoutParams.height + activity.statusBarInsetHeight.top
        header.requestLayout()
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
            val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(binding.recyclerViewContacts) {
                override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                    return listOf(deleteButton(addressBookListAdapter, position))
                }
            })

            itemTouchHelper.attachToRecyclerView(binding.recyclerViewContacts)
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: AddressBookViewState) {
//        TODO("Not yet implemented")
    }

    private fun deleteButton(addressBookListAdapter: AddressBookListAdapter, position: Int) : SwipeHelper.UnderlayButton {
        val button = SwipeHelper.UnderlayButton(
            requireContext(),
            chat.sphinx.resources.R.color.primaryRed,
            object : SwipeHelper.UnderlayButtonClickListener {
            override fun onClick() {
                addressBookListAdapter.removeAt(position)
            }
        })
        ContextCompat.getDrawable(requireContext(), R.drawable.ic_icon_delete)?.let {
            button.addIcon(it)
        }

        return button
    }

}
