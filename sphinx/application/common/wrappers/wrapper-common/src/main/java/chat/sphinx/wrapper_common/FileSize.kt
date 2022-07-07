package chat.sphinx.wrapper_common

import java.io.File
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.*


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

@JvmInline
value class FileSize(val value: Long)
