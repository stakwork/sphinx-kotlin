package chat.sphinx.dashboard.ui.feed.all

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.feed.FeedFollowingViewModel
import chat.sphinx.dashboard.ui.feed.FeedRecommendationsViewModel
import chat.sphinx.dashboard.ui.viewstates.FeedAllViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedRecommendation
import chat.sphinx.wrapper_feed.toJson
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject


@HiltViewModel
internal class FeedAllViewModel @Inject constructor(
    val dashboardNavigator: DashboardNavigator,
    private val repositoryDashboard: RepositoryDashboardAndroid<Any>,
    val moshi: Moshi,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedAllSideEffect,
        FeedAllViewState
        >(dispatchers, FeedAllViewState.NoRecommendations), FeedFollowingViewModel, FeedRecommendationsViewModel
{

    override val feedRecommendationsHolderViewStateFlow: MutableStateFlow<List<FeedRecommendation>> = MutableStateFlow(emptyList())

    init {
        loadFeedRecommendations()
    }

    override fun loadFeedRecommendations() {
        viewModelScope.launch(mainImmediate) {
            updateViewState(FeedAllViewState.Loading)

            repositoryDashboard.getRecommendedFeeds().collect { feedRecommended ->
                feedRecommendationsHolderViewStateFlow.value = feedRecommended.toList()

                if (feedRecommended.isNotEmpty()) {
                    updateViewState(FeedAllViewState.RecommendedList)
                } else updateViewState(FeedAllViewState.NoRecommendations)
            }
        }
    }

    override val feedsHolderViewStateFlow: StateFlow<List<Feed>> = flow {
        repositoryDashboard.getAllFeeds().collect { feeds ->
            emit(feeds.toList())
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    override fun feedSelected(feed: Feed) {
        @Exhaustive
        when (feed.feedType) {
            is FeedType.Podcast -> {
                goToPodcastPlayer(feed.chatId, feed.id, feed.feedUrl)
            }
            is FeedType.Video -> {
                goToVideoPlayer(feed.chatId, feed.id, feed.feedUrl)
            }
            is FeedType.Newsletter -> {
                goToNewsletterDetail(feed.chatId, feed.feedUrl)
            }
            is FeedType.Unknown -> {}
        }
    }

    private fun goToPodcastPlayer(
        chatId: ChatId,
        feedId: FeedId,
        feedUrl: FeedUrl
    ) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toPodcastPlayerScreen(
                chatId, feedId, feedUrl, 0
            )
        }
    }

    private fun goToNewsletterDetail(chatId: ChatId, feedUrl: FeedUrl) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toNewsletterDetail(chatId, feedUrl)
        }
    }

    private fun goToVideoPlayer(chatId: ChatId, feedId: FeedId, feedUrl: FeedUrl) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toVideoWatchScreen(chatId, feedId, feedUrl)
        }
    }

    override fun feedRecommendationSelected(feed: FeedRecommendation) {
        viewModelScope.launch(mainImmediate) {
            val recommendations = feedRecommendationsHolderViewStateFlow.value

            if (recommendations.isEmpty()) {
                return@launch
            }

            var feedRecommendationParamsList: MutableList<String> = mutableListOf()

            for (r in recommendations) {
                feedRecommendationParamsList.add(
                    r.toJson(moshi)
                )
            }

            dashboardNavigator.toCommonPlayerScreen(feedRecommendationParamsList, feed.id)
        }
    }

}
