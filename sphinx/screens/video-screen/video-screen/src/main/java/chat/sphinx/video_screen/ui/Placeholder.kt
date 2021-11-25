package chat.sphinx.video_screen.ui

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_feed.*
import java.sql.Date

class Placeholder {
    companion object {

        val youtubeFeed = Feed(
            FeedId("feedYoutubeItemId"),
            FeedType.Video,
            FeedTitle("Youtube we see a lot"),
            FeedDescription("Describing the things we see"),
            FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
            FeedAuthor("Youtube Channel"),
            null,
            PhotoUrl("https://cdn.mos.cms.futurecdn.net/8gzcr6RpGStvZFA2qRt4v6.jpg"),
            FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
            FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
            DateTime(Date.valueOf("2021-09-22")),
            DateTime(Date.valueOf("2021-09-22")),
            null,
            null,
            FeedItemsCount(0L),
            null,
            ChatId(0L),
        )

        val remoteVideoFeed = Feed(
            FeedId("feedItemId"),
            FeedType.Video,
            FeedTitle("Youtube we see a lot"),
            FeedDescription("Describing the things we see"),
            FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
            FeedAuthor("Normal Video Feed"),
            null,
            PhotoUrl("https://pbs.twimg.com/media/FEvdQm5XoAAcXgw?format=jpg&name=small"),
            FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
            FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
            DateTime(Date.valueOf("2021-09-22")),
            DateTime(Date.valueOf("2021-09-22")),
            null,
            null,
            FeedItemsCount(0L),
            null,
            ChatId(0L),
        )

        val youtubeFeedItem = FeedItem(
            FeedId("jNQXAC9IVRw"),
            FeedTitle("Youtube we see a lot"),
            FeedDescription("Describing the things we see"),
            DateTime(Date.valueOf("2021-09-22")),
            DateTime(Date.valueOf("2021-09-22")),
            FeedAuthor("Youtube Channel"),
            null,
            null,
            FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
            null,
            PhotoUrl("https://cdn.mos.cms.futurecdn.net/8gzcr6RpGStvZFA2qRt4v6.jpg"),
            PhotoUrl("https://cdn.mos.cms.futurecdn.net/8gzcr6RpGStvZFA2qRt4v6.jpg"),
            FeedUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
            FeedId("youtubeFeedId"),
        ).apply {
            this.feed = youtubeFeed
        }

        val remoteVideoFeedItem = FeedItem(
            FeedId("feedItemId"),
            FeedTitle("Something we see a lot"),
            FeedDescription("Describing the things we see"),
            DateTime(Date.valueOf("2021-09-22")),
            DateTime(Date.valueOf("2021-09-22")),
            FeedAuthor("Kgothatso"),
            null,
            null,
            FeedUrl("https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4"),
            null,
            PhotoUrl("https://pbs.twimg.com/media/FEvdQm5XoAAcXgw?format=jpg&name=small"),
            PhotoUrl("https://pbs.twimg.com/media/FEvdQm5XoAAcXgw?format=jpg&name=small"),
            FeedUrl("https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4"),
            FeedId("feedId"),
        ).apply {
            this.feed = remoteVideoFeed
        }
    }
}