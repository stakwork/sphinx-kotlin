package chat.sphinx.address_book.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.address_book.R
import chat.sphinx.address_book.databinding.LayoutAddressBookContactHolderBinding
import chat.sphinx.address_book.ui.AddressBookViewModel
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.resources.getRandomHexCode
import chat.sphinx.resources.setInitialsColor
import chat.sphinx.resources.setTextColorExt
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.getColorKey
import chat.sphinx.wrapper_contact.isBlocked
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AddressBookListAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: AddressBookViewModel,
    private val userColorsHelper: UserColorsHelper
): RecyclerView.Adapter<AddressBookListAdapter.AddressBookViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<Contact>,
        private val newList: List<Contact>,
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        @Volatile
        var sameList: Boolean = oldListSize == newListSize
            private set

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val same: Boolean =  try {
                oldList[oldItemPosition].let { old ->
                    newList[newItemPosition].let { new ->
                        old.id == new.id
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                false
            }

            if (sameList) {
                sameList = same
            }

            return same
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val same: Boolean = try {
                // the Contact is a data class so we can simply compare string
                // values to see if any fields have changed.
                oldList[oldItemPosition].toString() == newList[newItemPosition].toString()
            } catch (e: IndexOutOfBoundsException) {
                false
            }

            if (sameList) {
                sameList = same
            }

            return same
        }

    }

    private val addressBookContacts = ArrayList<Contact>(viewModel.currentViewState.list)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->
                if (addressBookContacts.isEmpty()) {
                    addressBookContacts.addAll(viewState.list)
                    this@AddressBookListAdapter.notifyDataSetChanged()
                } else {
                    val diff = Diff(addressBookContacts, viewState.list)

                    withContext(viewModel.dispatchers.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let {

                        if (!diff.sameList) {
                            addressBookContacts.clear()
                            addressBookContacts.addAll(viewState.list)
                            this@AddressBookListAdapter.notifyDataSetChanged()
                        }

                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return addressBookContacts.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressBookViewHolder {
        val binding = LayoutAddressBookContactHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return AddressBookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressBookViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    inner class AddressBookViewHolder(
        private val binding: LayoutAddressBookContactHolderBinding
    ): RecyclerView.ViewHolder(binding.root) {

        private var disposable: Disposable? = null
        private var dContact: Contact? = null

        init {
            binding.apply {
                layoutConstraintContactInfoContainer.setOnClickListener {
                    dContact?.let { contact ->
                        lifecycleOwner.lifecycleScope.launch {
                            viewModel.addressBookNavigator.toEditContactDetail(
                                contact.id
                            )
                        }
                    }
                }

                layoutConstraintContactInfoContainer.setOnLongClickListener {
                    viewModel.onItemLongClick()
                    return@setOnLongClickListener true
                }

                layoutConstraintDeleteButtonContainer.setOnClickListener {
                    dContact?.let { contact ->
                        lifecycleOwner.lifecycleScope.launch {
                            viewModel.confirmDeleteContact(contact) {
                                swipeRevealLayoutContact.close(false)
                            }
                        }
                    }
                }

                layoutConstraintBlockButtonContainer.setOnClickListener {
                    dContact?.let { contact ->
                        lifecycleOwner.lifecycleScope.launch {
                            viewModel.confirmToggleBlockContactState(contact) {
                                swipeRevealLayoutContact.close(true)
                            }
                        }
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val addressBookContact: Contact = addressBookContacts.getOrNull(position) ?: let {
                    dContact = null
                    return
                }
                dContact = addressBookContact
                disposable?.dispose()

                // Set Defaults
                textViewAddressBookHolderName.setTextColorExt(android.R.color.white)

                // Image
                addressBookContact.photoUrl.let { url ->

                    layoutAddressBookInitialHolder.apply {
                        imageViewChatPicture.goneIfFalse(url != null)
                        textViewInitials.goneIfFalse(url == null)
                    }

                    if (url != null) {
                        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                            imageLoader.load(
                                layoutAddressBookInitialHolder.imageViewChatPicture,
                                url.value,
                                imageLoaderOptions
                            )
                        }
                    } else {
                        layoutAddressBookInitialHolder.textViewInitials.text =
                            addressBookContact.alias?.value?.getInitials() ?: ""

                        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                            layoutAddressBookInitialHolder.textViewInitials
                                .setInitialsColor(
                                    Color.parseColor(
                                        userColorsHelper.getHexCodeForKey(
                                            addressBookContact.getColorKey(),
                                            layoutAddressBookInitialHolder.textViewInitials.context.getRandomHexCode()
                                        )
                                    ),
                                    R.drawable.chat_initials_circle
                                )
                        }
                    }

                }

                // Name
                textViewAddressBookHolderName.text = if (addressBookContact.alias != null) {
                    addressBookContact.alias?.value
                } else {
                    // Should never make it here, but just in case...
                    textViewAddressBookHolderName.setTextColorExt(R.color.primaryRed)
                    "ERROR: NULL NAME"
                }

                //Blocked
                layoutConstraintContactInfoContainer.alpha = if (addressBookContact.isBlocked()) 0.5f else 1.0f
                imageViewBlockedContactIcon.goneIfFalse(addressBookContact.isBlocked())

                textViewBlockButtonTitle.text = root.context.getString(
                    if (addressBookContact.isBlocked()) {
                        R.string.unblock_contact
                    } else {
                        R.string.block_contact
                    }
                )
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}