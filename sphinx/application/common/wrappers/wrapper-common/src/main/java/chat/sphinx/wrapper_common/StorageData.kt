package chat.sphinx.wrapper_common

data class StorageData(
    val usedStorage: FileSize,
    val totalStorage: FileSize,
    val freeStorage: FileSize,
    val images: FileSize,
    val video: FileSize,
    val audio: FileSize,
    val files: FileSize,
    val chats: FileSize,
    val podcasts: FileSize
)
