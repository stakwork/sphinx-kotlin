package chat.sphinx.podcast_player.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.podcast_player.databinding.LayoutEpisodeListFooterBinding

/**
 * Needed in order to have the last item of the RecyclerView
 * be able to scroll up over the device's navigation bar.
 * */
internal class PodcastEpisodesFooterAdapter(
    private val insetterActivity: InsetterActivity
): RecyclerView.Adapter<PodcastEpisodesFooterAdapter.FooterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = LayoutEpisodeListFooterBinding.inflate(
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
        private val binding: LayoutEpisodeListFooterBinding
    ): RecyclerView.ViewHolder(binding.root) {
        init {
            insetterActivity.addNavigationBarPadding(binding.layoutConstraintEpisodeListFooter)
        }
    }
}
