package chat.sphinx.wrapper_common


fun calculateStoragePercentage(storageData: StorageData): StoragePercentage {
    val totalStorage = storageData.freeStorage.value + storageData.images.value +
            storageData.video.value + storageData.audio.value +
            storageData.files.value

    return StoragePercentage(
        freeStorage = String.format("%.3f", storageData.freeStorage.value.toFloat() / totalStorage).toFloat(),
        image = String.format("%.3f", storageData.images.value.toFloat() / totalStorage).toFloat(),
        video = String.format("%.3f", storageData.video.value.toFloat() / totalStorage).toFloat(),
        audio = String.format("%.3f", storageData.audio.value.toFloat() / totalStorage).toFloat(),
        files = String.format("%.3f", storageData.files.value.toFloat() / totalStorage).toFloat()
    )
}

data class StoragePercentage(
    val freeStorage: Float,
    val image: Float,
    val video: Float,
    val audio: Float,
    val files: Float,
)