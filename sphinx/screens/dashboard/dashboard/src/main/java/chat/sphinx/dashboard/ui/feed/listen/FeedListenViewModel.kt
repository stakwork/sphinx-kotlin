package chat.sphinx.dashboard.ui.feed.listen

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.feed.FeedFollowingViewModel
import chat.sphinx.dashboard.ui.viewstates.FeedListenViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_feed.FeedType
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedListenViewModel @Inject constructor(
    val dashboardNavigator: DashboardNavigator,
    private val repositoryDashboard: RepositoryDashboardAndroid<Any>,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedListenSideEffect,
        FeedListenViewState
        >(dispatchers, FeedListenViewState.Default), FeedFollowingViewModel
{
    override val feedsHolderViewStateFlow: StateFlow<List<Feed>> = flow {
        repositoryDashboard.getAllFeedsOfType(FeedType.Podcast).collect { podcastFeeds ->
            emit(podcastFeeds.toList())
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun episodeItemSelected(episode: FeedItem) {
        episode.feed?.chat?.id?.let { chatId ->
            goToPodcastPlayer(chatId)
        }
    }

    override fun feedSelected(feed: Feed) {
        feed.chat?.id?.let { chatId ->
            goToPodcastPlayer(chatId)
        }
    }

    private fun goToPodcastPlayer(chatId: ChatId) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toPodcastPlayerScreen(
                chatId, 0
            )
        }
    }
}
