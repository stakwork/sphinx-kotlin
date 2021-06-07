package chat.sphinx.user_colors

import android.content.Context
import android.graphics.Color
import chat.sphinx.concept_user_colors.UserColors
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.withContext

class UserColorsImpl(
    context: Context,
    private val dispatchers: CoroutineDispatchers
) : UserColors(),
    CoroutineDispatchers by dispatchers{

    private val appContext: Context = context.applicationContext

    override suspend fun getHexCodeForKey(colorKey: String?, randomHexCode: String): String {
        var hexColorCode: String? = null

        colorKey?.let { colorKey ->
            withContext(dispatchers.io) {
                appContext.getSharedPreferences("sphinx_colors", Context.MODE_PRIVATE).let { sharedPrefs ->
                    sharedPrefs.getString(colorKey, null)?.let { savedHexCode ->
                        hexColorCode = savedHexCode
                    } ?: run {
                        hexColorCode = randomHexCode

                        sharedPrefs?.edit()?.let { editor ->
                            editor.putString(colorKey, randomHexCode).let { editor ->
                                if (!editor.commit()) {
                                    editor.apply()
                                }
                            }
                        }
                    }
                }
            }
        }
        return hexColorCode ?: randomHexCode
    }
}