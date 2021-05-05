package chat.sphinx.address_book.ui.adapter

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.address_book.databinding.LayoutAddressBookContactHolderBinding
import chat.sphinx.address_book.databinding.LayoutAddressBookHeaderContactHolderBinding
import chat.sphinx.address_book.ui.AddressBookViewModel
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.resources.R
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.resources.setTextColorExt
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_contact.Contact
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisorScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AddressBookListAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: AddressBookViewModel
): RecyclerView.Adapter<AddressBookListAdapter.AddressBookViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<Contact>,
        private val newList: List<Contact>,
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return oldList.size
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
    private val supervisor = OnStopSupervisorScope(lifecycleOwner)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        supervisor.scope().launch(viewModel.dispatchers.mainImmediate) {
            viewModel.collectViewState { viewState ->

                if (addressBookContacts.isEmpty()) {
                    addressBookContacts.addAll(viewState.list)
                    this@AddressBookListAdapter.notifyDataSetChanged()
                } else {
                    val diff = Diff(addressBookContacts, viewState.list)

                    withContext(viewModel.dispatchers.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let { result ->

                        if (!diff.sameList) {
                            addressBookContacts.clear()
                            addressBookContacts.addAll(viewState.list)
                            result.dispatchUpdatesTo(this@AddressBookListAdapter)
                        }

                    }
                }
            }
        }
    }

    fun removeAt(position: Int) {
        val contact = addressBookContacts[position]

        viewModel.deleteContact(contact)

        notifyItemRemoved(position)
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

                    layoutAddressBookInitialHolder.imageViewChatPicture.goneIfFalse(url != null)
                    layoutAddressBookInitialHolder.textViewInitials.goneIfFalse(url == null)

                    if (url != null) {
                        supervisor.scope().launch(viewModel.dispatchers.mainImmediate) {
                            imageLoader.load(
                                layoutAddressBookInitialHolder.imageViewChatPicture,
                                url.value,
                                imageLoaderOptions
                            )
                        }
                    } else {
                        layoutAddressBookInitialHolder.textViewInitials.text =
                            addressBookContact.alias?.value?.getInitials() ?: ""
                        layoutAddressBookInitialHolder.textViewInitials
                            .setBackgroundRandomColor(R.drawable.chat_initials_circle)
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
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}