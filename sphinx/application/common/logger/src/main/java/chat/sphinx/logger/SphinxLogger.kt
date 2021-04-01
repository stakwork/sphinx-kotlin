package chat.sphinx.logger

@Suppress("NOTHING_TO_INLINE")
inline fun SphinxLogger.debug(tag: String, message: String) {
    log(tag, message, LogType.Debug)
}

@Suppress("NOTHING_TO_INLINE")
inline fun SphinxLogger.info(tag: String, message: String) {
    log(tag, message, LogType.Info)
}

@Suppress("NOTHING_TO_INLINE")
inline fun SphinxLogger.warn(tag: String, message: String) {
    log(tag, message, LogType.Warning)
}

@Suppress("NOTHING_TO_INLINE")
inline fun SphinxLogger.verbose(tag: String, message: String) {
    log(tag, message, LogType.Verbose)
}

abstract class SphinxLogger {
    abstract fun log(tag: String, message: String, type: LogType)
}
