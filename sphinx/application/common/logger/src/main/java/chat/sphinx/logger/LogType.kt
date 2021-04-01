package chat.sphinx.logger

sealed class LogType {
    object Debug: LogType()
    object Info: LogType()
    object Warning: LogType()
}
