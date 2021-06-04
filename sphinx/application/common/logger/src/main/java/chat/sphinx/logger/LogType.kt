package chat.sphinx.logger

sealed class LogType {
    object Debug: LogType()
    object Exception: LogType()
    object Info: LogType()
    object Warning: LogType()
    object Verbose: LogType()
}
