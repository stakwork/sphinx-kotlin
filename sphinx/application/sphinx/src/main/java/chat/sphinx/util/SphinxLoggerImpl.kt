package chat.sphinx.util

import android.util.Log
import app.cash.exhaustive.Exhaustive
import chat.sphinx.logger.LogType
import chat.sphinx.logger.SphinxLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SphinxLoggerImpl @Inject constructor(): SphinxLogger() {
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
