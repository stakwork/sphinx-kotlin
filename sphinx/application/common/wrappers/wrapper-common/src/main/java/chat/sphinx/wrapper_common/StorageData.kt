package chat.sphinx.wrapper_common

data class StorageData(
    val totalStorage: FileSize,
    val images: FileSize,
    val video: FileSize,
    val audio: FileSize,
    val files: FileSize,
)
