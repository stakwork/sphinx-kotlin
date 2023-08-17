package chat.sphinx.dashboard.ui.feed.read

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.feed.FeedFollowingViewModel
import chat.sphinx.dashboard.ui.feed.FeedRecentlyPlayedViewModel
import chat.sphinx.dashboard.ui.viewstates.FeedReadViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.isTrue
import chat.sphinx.wrapper_common.time
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class FeedReadViewModel @Inject constructor(
    private val app: Application,
    val dashboardNavigator: DashboardNavigator,
    private val repositoryDashboard: RepositoryDashboardAndroid<Any>,
    private val actionsRepository: ActionsRepository,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedReadSideEffect,
        FeedReadViewState
        >(dispatchers, FeedReadViewState.Idle), FeedFollowingViewModel, FeedRecentlyPlayedViewModel
{
    private val _feedsHolderViewStateFlow: MutableStateFlow<List<Feed>> by lazy {
        MutableStateFlow(listOf())
    }

    override val feedsHolderViewStateFlow: StateFlow<List<Feed>>
        get() = _feedsHolderViewStateFlow

    private val _lastPlayedFeedsHolderViewStateFlow: MutableStateFlow<List<Feed>> by lazy {
        MutableStateFlow(listOf())
    }

    override val lastPlayedFeedsHolderViewStateFlow: StateFlow<List<Feed>>
        get() = _lastPlayedFeedsHolderViewStateFlow

    init {
        viewModelScope.launch(mainImmediate) {
            repositoryDashboard.getAllFeedsOfType(FeedType.Newsletter).collect { feeds ->

                _feedsHolderViewStateFlow.value = feeds.toList()
                    .filter { it.subscribed.isTrue() || it.chatId.value.toInt() != ChatId.NULL_CHAT_ID }
                    .sortedByDescending { it.lastPublished?.datePublished?.time ?: 0 }

                _lastPlayedFeedsHolderViewStateFlow.value = feeds.toList()
                    .filter { it.lastPlayed != null }
                    .sortedWith(compareByDescending<Feed> { it.lastPlayed?.time }.thenByDescending { it.lastPublished?.datePublished?.time ?: 0 })
            }
        }
    }

    override fun recentlyPlayedSelected(feed: Feed) {
        feedSelected(feed)
    }

    override fun feedSelected(feed: Feed) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toNewsletterDetail(feed.chatId, feed.feedUrl)
        }
    }

    fun newsletterItemSelected(item: FeedItem) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toWebViewDetail(
                item?.feed?.chat?.id ?: item?.feed?.chatId,
                app.getString(R.string.newsletter_article),
                item.enclosureUrl,
                item.feedId,
                item.id
            )
        }
        actionsRepository.trackNewsletterConsumed(item.id)
    }
}
