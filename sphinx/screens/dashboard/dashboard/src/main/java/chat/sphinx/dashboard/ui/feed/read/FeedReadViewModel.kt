package chat.sphinx.dashboard.ui.feed.read

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.feed.FeedFollowingViewModel
import chat.sphinx.dashboard.ui.viewstates.FeedReadViewState
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_common.feed.FeedType
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
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedReadSideEffect,
        FeedReadViewState
        >(dispatchers, FeedReadViewState.Idle), FeedFollowingViewModel
{
    override val feedsHolderViewStateFlow: StateFlow<List<Feed>> = flow {
        repositoryDashboard.getAllFeedsOfType(FeedType.Newsletter).collect { newsletterFeeds ->
            emit(newsletterFeeds.toList())
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    override fun feedSelected(feed: Feed) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toNewsletterDetail(feed.chatId)
        }
    }

    fun newsletterItemSelected(episode: FeedItem) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toWebViewDetail(
                app.getString(R.string.newsletter_article),
                episode.enclosureUrl
            )
        }
    }
}
