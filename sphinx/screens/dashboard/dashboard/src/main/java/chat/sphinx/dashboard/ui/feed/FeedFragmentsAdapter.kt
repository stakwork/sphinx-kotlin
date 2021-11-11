package chat.sphinx.dashboard.ui.feed

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import chat.sphinx.dashboard.ui.feed.all.FeedAllFragment
import chat.sphinx.dashboard.ui.feed.listen.FeedListenFragment

class FeedFragmentsAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    companion object {
        const val CHIP_ALL_POSITION = 0
        const val CHIP_LISTEN_POSITION = 1
        const val CHIP_WATCH_POSITION = 2
        const val CHIP_READ_POSITION = 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            CHIP_ALL_POSITION -> {
                FeedAllFragment.newInstance()
            }
            CHIP_LISTEN_POSITION -> {
                FeedListenFragment.newInstance()
            }
            CHIP_READ_POSITION -> {
                FeedAllFragment.newInstance()
            }
            CHIP_WATCH_POSITION -> {
                FeedAllFragment.newInstance()
            }
            else ->  {
                FeedAllFragment.newInstance()
            }
        }
    }

    override fun getItemCount(): Int {
        return 5
    }
}