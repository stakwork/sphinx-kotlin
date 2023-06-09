package chat.sphinx.wrapper_common

import java.io.File
import java.text.CharacterIterator
import java.text.DecimalFormat
import java.text.StringCharacterIterator
import java.util.*


@Suppress("NOTHING_TO_INLINE")
inline fun Long.toFileSize(): FileSize? {
    return if (this > 0) FileSize(this) else null
}

@Suppress("NOTHING_TO_INLINE")
inline fun FileSize.asFormattedString(): String{

    var bytes = value
    if (-1000 < bytes && bytes < 1000) {
        return "$bytes B"
    }
    val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
    while (bytes <= -999950 || bytes >= 999950) {
        bytes /= 1000
        ci.next()
    }
    return String.format(Locale.ENGLISH,"%.1f %cB", bytes / 1000.0, ci.current())
}

@Suppress("NOTHING_TO_INLINE")
inline fun FileSize.calculateSize(): String {
    val totalSize = value

    val kb: Double = 1024.0
    val mb: Double = kb * 1024
    val gb: Double = mb * 1024

    val decimalFormat = DecimalFormat("#.##")

    return when {
        totalSize < kb -> "$totalSize Bytes"
        totalSize < mb -> "${decimalFormat.format(totalSize / kb)} KB"
        totalSize < gb -> "${decimalFormat.format(totalSize / mb)} MB"
        else -> "${decimalFormat.format(totalSize / gb)} GB"
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun List<FileSize>.calculateTotalSize(): String {
    var totalSize = 0L

    for (fileSize in this) {
        totalSize += fileSize.value
    }

    val kb: Double = 1024.0
    val mb: Double = kb * 1024
    val gb: Double = mb * 1024

    val decimalFormat = DecimalFormat("#.##")

    return when {
        totalSize < kb -> "$totalSize Bytes"
        totalSize < mb -> "${decimalFormat.format(totalSize / kb)} KB"
        totalSize < gb -> "${decimalFormat.format(totalSize / mb)} MB"
        else -> "${decimalFormat.format(totalSize / gb)} GB"
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.calculateLongSize(): Long {
    val parts = this.split(" ")
    val size = parts[0].toDouble()
    val unit = parts[1].uppercase()

    return when (unit) {
        "B" -> size.toLong()
        "KB" -> (size * 1024).toLong()
        "MB" -> (size * 1024 * 1024).toLong()
        "GB" -> (size * 1024 * 1024 * 1024).toLong()
        else -> throw IllegalArgumentException("Invalid file size unit: $unit")
    }
}

@JvmInline
value class FileSize(val value: Long)
