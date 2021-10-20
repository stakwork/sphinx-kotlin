package chat.sphinx.address_book.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
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
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.resources.inputMethodManager
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
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

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var userColorsHelper: UserColorsHelper

    override val viewModel: AddressBookViewModel by viewModels()
    override val binding: FragmentAddressBookBinding by viewBinding(FragmentAddressBookBinding::bind)

    private val header: ConstraintLayout
        get() = binding.layoutAddressBookHeader.layoutConstraintAddressBookHeader
    private val headerNavBack: TextView
        get() = binding.layoutAddressBookHeader.textViewAddressBookHeaderNavBack

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        headerNavBack.setOnClickListener {
            lifecycleScope.launch {
                viewModel.addressBookNavigator.popBackStack()
            }
        }

        binding.layoutAddressBookButtonAddFriend.layoutConstraintAddressBookButtonAddFriend.setOnClickListener {
            lifecycleScope.launch {
                viewModel.addressBookNavigator.toAddFriendDetail()
            }
        }

        setupAddressBookHeader()
        setupContacts()
        setupSearch()
    }

    private fun setupAddressBookHeader() {
        val activity = (requireActivity() as InsetterActivity)
        activity.addStatusBarPadding(header)

        header.layoutParams.height = header.layoutParams.height + activity.statusBarInsetHeight.top
        header.requestLayout()
    }

    private fun setupContacts() {
        val addressBookListAdapter = AddressBookListAdapter(imageLoader, viewLifecycleOwner, onStopSupervisor, viewModel, userColorsHelper)
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

    private fun setupSearch() {
        binding.layoutAddressBookSearchBar.apply {
            editTextAddressBookSearch.addTextChangedListener { editable ->
                buttonAddressBookSearchClear.goneIfFalse(editable.toString().isNotEmpty())

                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    viewModel.updateAddressBookListFilter(
                        if (editable.toString().isNotEmpty()) {
                            AddressBookFilter.FilterBy(editable.toString())
                        } else {
                            AddressBookFilter.ClearFilter
                        }
                    )
                }
            }

            editTextAddressBookSearch.setOnEditorActionListener(object: TextView.OnEditorActionListener {
                override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                    if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                        editTextAddressBookSearch.let { editText ->
                            binding.root.context.inputMethodManager?.let { imm ->
                                if (imm.isActive(editText)) {
                                    imm.hideSoftInputFromWindow(editText.windowToken, 0)
                                    editText.clearFocus()
                                }
                            }
                        }
                        return true
                    }
                    return false
                }
            })

            buttonAddressBookSearchClear.setOnClickListener {
                editTextAddressBookSearch.setText("")
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: AddressBookViewState) {
//        TODO("Not yet implemented")
    }

    private fun deleteButton(addressBookListAdapter: AddressBookListAdapter, position: Int) : SwipeHelper.UnderlayButton {
        val button = SwipeHelper.UnderlayButton(
            requireContext(),
            chat.sphinx.resources.R.color.badgeRed,
            object : SwipeHelper.UnderlayButtonClickListener {
            override fun onClick() {
                addressBookListAdapter.removeAt(position)
            }
        })
        ContextCompat.getDrawable(requireContext(), R.drawable.ic_icon_delete)?.let {
            button.addIcon(it, requireContext().resources.getDimension(R.dimen.recycler_view_holder_delete_button_width))
        }

        return button
    }

}
