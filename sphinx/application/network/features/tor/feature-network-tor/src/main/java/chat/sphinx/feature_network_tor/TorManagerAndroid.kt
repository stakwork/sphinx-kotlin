package chat.sphinx.feature_network_tor

import android.app.Application
import chat.sphinx.concept_network_tor.TorManager
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.build_config.BuildConfigVersionCode

class TorManagerAndroid(
    application: Application,
    buildConfigDebug: BuildConfigDebug,
    buildConfigVersionCode: BuildConfigVersionCode,
): TorManager() {
}