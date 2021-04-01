package chat.sphinx.logger

abstract class SphinxLogger {
    abstract fun log(tag: String, message: String)
}
