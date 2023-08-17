package chat.sphinx.newsletter_detail.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.newsletter_detail.R
import chat.sphinx.newsletter_detail.navigation.NewsletterDetailNavigator
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.isTrue
import chat.sphinx.wrapper_common.feed.toFeedUrl
import chat.sphinx.wrapper_common.feed.toSubscribed
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val NewsletterDetailFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
internal class NewsletterDetailViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val app: Application,
    private val chatRepository: ChatRepository,
    private val feedRepository: FeedRepository,
    private val actionsRepository: ActionsRepository,
    val navigator: NewsletterDetailNavigator
): BaseViewModel<NewsletterDetailViewState>(dispatchers, NewsletterDetailViewState.Idle)
{
    private val args: NewsletterDetailFragmentArgs by savedStateHandle.navArgs()

    private val newsletterSharedFlow: SharedFlow<Feed?> = flow {
        emitAll(feedRepository.getFeedByChatId(args.chatId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    init {
        viewModelScope.launch(mainImmediate) {
            newsletterSharedFlow.collect { feed ->
                feed?.let { nnFeed ->
                    updateViewState(
                        NewsletterDetailViewState.FeedLoaded(
                            nnFeed.imageUrlToShow,
                            nnFeed.title,
                            nnFeed.description,
                            nnFeed.items
                        )
                    )
                }
            }
        }

        updateFeedContentInBackground()
    }

    private fun updateFeedContentInBackground() {
        viewModelScope.launch(io) {
            val chat = chatRepository.getChatById(args.chatId).firstOrNull()
            val newsletterFeed = newsletterSharedFlow.firstOrNull()
            val chatHost = chat?.host ?: ChatHost(Feed.TRIBES_DEFAULT_SERVER_URL)
            val subscribed = newsletterFeed?.subscribed?.isTrue() == true

            args.argFeedUrl.toFeedUrl()?.let { feedUrl ->
                feedRepository.updateFeedContent(
                    chatId = chat?.id ?: ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    host = chatHost,
                    feedUrl = feedUrl,
                    chatUUID = chat?.uuid,
                    subscribed = subscribed.toSubscribed(),
                    currentItemId = null
                )
            }
        }
    }

    fun newsletterItemSelected(item: FeedItem) {
        viewModelScope.launch(mainImmediate) {
            navigator.toWebViewDetail(
                item?.feed?.chat?.id ?: item?.feed?.chatId,
                app.getString(R.string.newsletter_article),
                item.enclosureUrl,
                item.feedId,
                item.id
            )
        }
        actionsRepository.trackNewsletterConsumed(item.id)
        feedRepository.updateLastPlayed(item.feedId)
    }
}
