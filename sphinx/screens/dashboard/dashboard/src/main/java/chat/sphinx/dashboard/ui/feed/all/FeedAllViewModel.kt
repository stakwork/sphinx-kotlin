package chat.sphinx.dashboard.ui.feed.all

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.feed.FeedFollowingViewModel
import chat.sphinx.dashboard.ui.viewstates.FeedAllViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_feed.Feed
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
        repositoryDashboard.getAllFeeds().collect { podcastFeeds ->
            emit(podcastFeeds.toList())
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

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
