package chat.sphinx.logger

@Suppress("NOTHING_TO_INLINE")
inline fun SphinxLogger.d(tag: String, message: String) {
    log(tag, message, LogType.Debug)
}

@Suppress("NOTHING_TO_INLINE")
inline fun SphinxLogger.e(tag: String, message: String, e: Exception?) {
    log(tag, message, LogType.Exception, e)
}

@Suppress("NOTHING_TO_INLINE")
inline fun SphinxLogger.i(tag: String, message: String) {
    log(tag, message, LogType.Info)
}

@Suppress("NOTHING_TO_INLINE")
inline fun SphinxLogger.w(tag: String, message: String, e: Exception? = null) {
    log(tag, message, LogType.Warning, e)
}

@Suppress("NOTHING_TO_INLINE")
inline fun SphinxLogger.v(tag: String, message: String) {
    log(tag, message, LogType.Verbose)
}

abstract class SphinxLogger {
    abstract fun log(tag: String, message: String, type: LogType, throwable: Throwable? = null)
}
