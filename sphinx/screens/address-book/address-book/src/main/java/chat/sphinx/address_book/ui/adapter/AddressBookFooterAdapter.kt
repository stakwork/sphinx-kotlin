package chat.sphinx.address_book.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.address_book.databinding.LayoutAddressBookContactsFooterBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding

/**
 * Needed in order to have the last item of the RecyclerView
 * be able to scroll up over the device's navigation bar.
 * */
internal class AddressBookFooterAdapter(
    private val insetterActivity: InsetterActivity
): RecyclerView.Adapter<AddressBookFooterAdapter.FooterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = LayoutAddressBookContactsFooterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return FooterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {}

    override fun getItemCount(): Int {
        return 1
    }

    inner class FooterViewHolder(
        private val binding: LayoutAddressBookContactsFooterBinding
    ): RecyclerView.ViewHolder(binding.root) {
        init {
            insetterActivity.addNavigationBarPadding(binding.layoutConstraintAddressBookContactsFooter)
        }
    }
}
