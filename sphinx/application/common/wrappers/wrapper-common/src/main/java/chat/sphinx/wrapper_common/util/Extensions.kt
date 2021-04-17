package chat.sphinx.wrapper_common.util

@Suppress("NOTHING_TO_INLINE")
inline fun String.getInitials(charLimit: Int = 2): String {
    val sb = StringBuilder()
    this.split(' ').let { splits ->
        for ((index, split) in splits.withIndex()) {
            if (index < charLimit) {
                sb.append(split.firstOrNull() ?: "")
            } else {
                break
            }
        }
    }
    return sb.toString()
}