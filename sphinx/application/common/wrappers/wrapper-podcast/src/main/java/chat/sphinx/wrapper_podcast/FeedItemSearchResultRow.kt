package chat.sphinx.wrapper_podcast

data class FeedItemSearchResultRow(
    val feedSearchResult: FeedItemSearchResult?,
    val isSectionHeader: Boolean,
    val isFollowingSection: Boolean,
    val isLastOnSection: Boolean
)