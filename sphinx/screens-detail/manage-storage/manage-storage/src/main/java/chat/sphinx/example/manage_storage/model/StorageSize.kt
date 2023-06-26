package chat.sphinx.example.manage_storage.model

data class StorageSize(
    val usedStorage: String,
    val freeStorage: String?,
    val image: String,
    val video: String,
    val audio: String,
    val files: String,
    val chats: String,
    val podcasts: String
)
