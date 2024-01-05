package chat.sphinx.wrapper_common

import java.util.Locale


fun calculateStoragePercentage(storageData: StorageData): StoragePercentage {
    val freeStorage = storageData.freeStorage?.value ?: 0L
    val totalStorage = freeStorage + storageData.images.totalSize.value +
            storageData.video.totalSize.value + storageData.audio.totalSize.value +
            storageData.files.totalSize.value

    return StoragePercentage(
        freeStorage = String.format(Locale.ENGLISH, "%.3f", freeStorage.toFloat() / totalStorage).toFloat(),
        image = String.format(Locale.ENGLISH, "%.3f", storageData.images.totalSize.value.toFloat() / totalStorage).toFloat(),
        video = String.format(Locale.ENGLISH, "%.3f", storageData.video.totalSize.value.toFloat() / totalStorage).toFloat(),
        audio = String.format(Locale.ENGLISH, "%.3f", storageData.audio.totalSize.value.toFloat() / totalStorage).toFloat(),
        files = String.format(Locale.ENGLISH, "%.3f", storageData.files.totalSize.value.toFloat() / totalStorage).toFloat()
    )
}

fun calculateUsedStoragePercentage(storageData: StorageData): Float {
    val usedStorage = storageData.usedStorage.value
    val freeStorage = storageData.freeStorage?.value ?: 0L
    val totalStorage = usedStorage + freeStorage
    return String.format("%.3f", usedStorage.toFloat() / totalStorage).toFloat()
}

data class StoragePercentage(
    val freeStorage: Float,
    val image: Float,
    val video: Float,
    val audio: Float,
    val files: Float,
)