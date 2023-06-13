package chat.sphinx.wrapper_common

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.message.MessageId
import java.io.File

fun calculateUserStorageLimit(seekBarValue: Int, freeStorage: Long): Long {
    return freeStorage * seekBarValue / 100
}

data class StorageData(
    val usedStorage: FileSize,
    val freeStorage: FileSize?,
    val chatsStorage: FileSize,
    val podcastsStorage: FileSize,
    val images: ImageStorage,
    val video: VideoStorage,
    val audio: AudioStorage,
    val files: FilesStorage,
)

data class ImageStorage(
    val totalSize: FileSize,
    val fileList: List<File>,
    val items: Map<ChatId, List<MessageId>>
)
data class VideoStorage(
    val totalSize: FileSize,
    val fileList: List<File>,
    val items: Map<ChatId, List<MessageId>>
)
data class AudioStorage(
    val totalSize: FileSize,
    val fileList: List<File>,
    val chatItems: Map<ChatId, List<MessageId>>,
    val feedList: List<FeedId>
)

data class FilesStorage(
    val totalSize: FileSize,
    val fileList: List<File>,
    val items: Map<ChatId, List<MessageId>>
)