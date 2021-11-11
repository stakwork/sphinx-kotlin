package chat.sphinx.dashboard.ui.feed

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import chat.sphinx.dashboard.ui.feed.all.FeedAllFragment
import chat.sphinx.dashboard.ui.feed.listen.FeedListenFragment

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class FeedFragmentsAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm) {

    companion object {
        const val CHIP_ALL_POSITION = 0
        const val CHIP_LISTEN_POSITION = 1
        const val CHIP_WATCH_POSITION = 2
        const val CHIP_READ_POSITION = 3
    }

    override fun getItem(position: Int): Fragment {
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

    override fun getPageTitle(position: Int): CharSequence {
        return ""
    }

    override fun getCount(): Int {
        return 5
    }
}