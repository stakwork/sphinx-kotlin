package chat.sphinx.util

import android.util.Log
import app.cash.exhaustive.Exhaustive
import chat.sphinx.logger.LogType
import chat.sphinx.logger.SphinxLogger

class SphinxLoggerImpl: SphinxLogger() {
    override fun log(tag: String, message: String, type: LogType) {

        @Exhaustive
        when (type) {
            LogType.Debug -> {
                Log.d(tag, message)
            }
            LogType.Info -> {
                Log.i(tag, message)
            }
            LogType.Warning -> {
                Log.w(tag, message)
            }
        }
    }
}
