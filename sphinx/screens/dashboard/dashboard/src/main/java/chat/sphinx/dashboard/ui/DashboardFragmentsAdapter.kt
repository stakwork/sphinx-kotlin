package chat.sphinx.dashboard.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.ui.feed.FeedFragment
import chat.sphinx.wrapper_chat.ChatType

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class DashboardFragmentsAdapter(
    private val fragment: Fragment
) : FragmentStateAdapter(fragment) {

    companion object {
        const val FEED_TAB_POSITION = 0
        const val FRIENDS_TAB_POSITION = 1
        const val TRIBES_TAB_POSITION = 2
        const val FIRST_INIT = 3

        val TAB_TITLES = arrayOf(
            R.string.dashboard_feed_tab_name,
            R.string.dashboard_friends_tab_name,
            R.string.dashboard_tribes_tab_name,
        )
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            FEED_TAB_POSITION -> {
                FeedFragment.newInstance()
            }
            FRIENDS_TAB_POSITION -> {
                ChatListFragment.newInstance(
                    chatListType = ChatType.Conversation
                )
            }
            TRIBES_TAB_POSITION -> {
                ChatListFragment.newInstance(
                    chatListType = ChatType.Tribe
                )
            }
            else ->  {
                ChatListFragment.newInstance()
            }
        }
    }

    fun getPageTitle(position: Int): CharSequence? {
        return fragment.resources.getString(TAB_TITLES[position])
    }

    override fun getItemCount(): Int {
        return TAB_TITLES.size
    }
}