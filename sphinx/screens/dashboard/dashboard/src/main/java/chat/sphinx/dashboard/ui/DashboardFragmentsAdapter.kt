package chat.sphinx.dashboard.ui

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import chat.sphinx.dashboard.R
import chat.sphinx.wrapper_chat.ChatType

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class DashboardFragmentsAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm) {

    companion object {
        const val FEED_TAB_POSITION = 0
        const val FRIENDS_TAB_POSITION = 1
        const val TRIBES_TAB_POSITION = 2

        val TAB_TITLES = arrayOf(
            R.string.dashboard_feed_tab_name,
            R.string.dashboard_friends_tab_name,
            R.string.dashboard_tribes_tab_name,
        )
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                FeedFragment.newInstance()
            }
            1 -> {
                ChatListFragment.newInstance(
                    chatListType = ChatType.Conversation
                )
            }
            2 -> {
                ChatListFragment.newInstance(
                    chatListType = ChatType.Tribe
                )
            }
            else ->  {
                ChatListFragment.newInstance()
            }
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        return TAB_TITLES.size
    }
}