package chat.sphinx.util

import android.util.Log
import app.cash.exhaustive.Exhaustive
import chat.sphinx.logger.LogType
import chat.sphinx.logger.SphinxLogger
import io.matthewnelson.build_config.BuildConfigDebug

class SphinxLoggerImpl(private val buildConfigDebug: BuildConfigDebug): SphinxLogger() {

    override fun log(tag: String, message: String, type: LogType, throwable: Throwable?) {
        @Exhaustive
        when (type) {
            LogType.Debug -> {
                if (buildConfigDebug.value) {
                    Log.d(tag, message)
                }
            }
            LogType.Exception -> {
                Log.e(tag, message, throwable)
            }
            LogType.Info -> {
                if (buildConfigDebug.value) {
                    Log.i(tag, message)
                }
            }
            LogType.Warning -> {
                Log.w(tag, message, throwable)
            }
            LogType.Verbose -> {
                if (buildConfigDebug.value) {
                    Log.v(tag, message)
                }
            }
        }
    }
}
