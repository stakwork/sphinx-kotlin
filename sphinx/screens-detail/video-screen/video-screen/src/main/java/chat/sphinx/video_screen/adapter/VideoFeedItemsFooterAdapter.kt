package chat.sphinx.video_screen.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.video_screen.databinding.LayoutVideoListFooterBinding

/**
 * Needed in order to have the last item of the RecyclerView
 * be able to scroll up over the device's navigation bar.
 * */
internal class VideoFeedItemsFooterAdapter(
    private val insetterActivity: InsetterActivity
): RecyclerView.Adapter<VideoFeedItemsFooterAdapter.FooterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = LayoutVideoListFooterBinding.inflate(
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
        binding: LayoutVideoListFooterBinding
    ): RecyclerView.ViewHolder(binding.root) {
        init {
            insetterActivity.addNavigationBarPadding(binding.layoutConstraintVideoListFooter)
        }
    }
}
