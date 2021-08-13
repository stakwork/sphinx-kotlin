package chat.sphinx.user_colors_helper

import android.content.Context
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class ColorHolder(val key: String, val color: String)

class UserColorsHelperImpl(
    context: Context,
    private val dispatchers: CoroutineDispatchers
) : UserColorsHelper(),
    CoroutineDispatchers by dispatchers{

    private val appContext: Context = context.applicationContext

    private val colorsSharedPreferences = appContext.getSharedPreferences("sphinx_colors", Context.MODE_PRIVATE)

    companion object {
        private const val CACHE_SIZE = 100

        private val colors: MutableList<ColorHolder> = ArrayList(CACHE_SIZE)
        private val lock = Mutex()
        private var counter = 0
    }

    override suspend fun getHexCodeForKey(
        colorKey: String,
        randomHexColorCode: String
    ): String {
        return lock.withLock {

            val cachedColor: String? = withContext(dispatchers.default) {
                for (color in colors) {
                    if (color.key == colorKey) {
                        return@withContext color.color
                    }
                }

                null
            }

            if (cachedColor != null) {
                return cachedColor
            }

            val colorHexCode: String = withContext(dispatchers.io) {
                colorsSharedPreferences.let { sharedPrefs ->
                    sharedPrefs.getString(colorKey, null) ?: run {
                        sharedPrefs?.edit()?.let { editor ->

                            editor.putString(colorKey, randomHexColorCode)

                            if (!editor.commit()) {
                                editor.apply()
                            }
                        }

                        randomHexColorCode
                    }
                }
            }

            updateColorHolderCache(ColorHolder(colorKey, colorHexCode))

            colorHexCode
        }
    }

    private suspend fun updateColorHolderCache(colorHolder: ColorHolder) {
        colors.add(counter, colorHolder)

        if (counter < CACHE_SIZE - 1 /* last index */) {
            counter++
        } else {
            counter = 0
        }
    }

}