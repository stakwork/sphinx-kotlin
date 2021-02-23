package chat.sphinx

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SphinxApp: Application() {

    override fun onCreate() {
        super.onCreate()
    }
}