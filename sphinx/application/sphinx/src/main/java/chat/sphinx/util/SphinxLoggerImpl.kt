package chat.sphinx.util

import android.util.Log
import app.cash.exhaustive.Exhaustive
import chat.sphinx.logger.LogType
import chat.sphinx.logger.SphinxLogger
import io.matthewnelson.build_config.BuildConfigDebug
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SphinxLoggerImpl @Inject constructor(
    private val buildConfigDebug: BuildConfigDebug,
): SphinxLogger() {

    override fun log(tag: String, message: String, type: LogType) {
        @Exhaustive
        when (type) {
            LogType.Debug -> {
                if (buildConfigDebug.value) {
                    Log.d(tag, message)
                }
            }
            LogType.Info -> {
                if (buildConfigDebug.value) {
                    Log.i(tag, message)
                }
            }
            LogType.Warning -> {
                Log.w(tag, message)
            }
            LogType.Verbose -> {
                if (buildConfigDebug.value) {
                    Log.v(tag, message)
                }
            }
        }
    }
}
