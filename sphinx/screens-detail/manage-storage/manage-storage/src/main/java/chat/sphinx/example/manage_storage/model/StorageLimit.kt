package chat.sphinx.example.manage_storage.model

data class StorageLimit(
    val seekBarProgress: Int,
    val usedStorage: String?,
    val userStorageLimit: String?,
    val freeStorage: String?,
    val undersized: String?,
    val progressBarPercentage: Float
)