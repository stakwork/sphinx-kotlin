package chat.sphinx.dashboard.ui.feed.all

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.feed.FeedFollowingViewModel
import chat.sphinx.dashboard.ui.viewstates.FeedAllViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_feed.Feed
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject


@HiltViewModel
internal class FeedAllViewModel @Inject constructor(
    val dashboardNavigator: DashboardNavigator,
    private val repositoryDashboard: RepositoryDashboardAndroid<Any>,
    dispatchers: CoroutineDispatchers,
): SideEffectViewModel<
        Context,
        FeedAllSideEffect,
        FeedAllViewState
        >(dispatchers, FeedAllViewState.Default), FeedFollowingViewModel
{

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
                goToPodcastPlayer(feed.chatId, feed.feedUrl)
            }
            is FeedType.Video -> {
                viewModelScope.launch(mainImmediate) {
                    submitSideEffect(
                        FeedAllSideEffect.Notify("Video Not supported yet")
                    )
                }
            }
            is FeedType.Newsletter -> {
                goToNewsletterDetail(feed.chatId, feed.feedUrl)
            }
            is FeedType.Unknown -> {}
        }
    }

    private fun goToPodcastPlayer(chatId: ChatId, feedUrl: FeedUrl) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toPodcastPlayerScreen(
                chatId, feedUrl, 0
            )
        }
    }

    private fun goToNewsletterDetail(chatId: ChatId, feedUrl: FeedUrl) {
        viewModelScope.launch(mainImmediate) {
            dashboardNavigator.toNewsletterDetail(chatId, feedUrl)
        }
    }
}
