package chat.sphinx.user_colors_helper

import android.content.Context
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.withContext

class UserColorsHelperImpl(
    context: Context,
    private val dispatchers: CoroutineDispatchers
) : UserColorsHelper(),
    CoroutineDispatchers by dispatchers{

    private val appContext: Context = context.applicationContext

    override suspend fun getHexCodeForKey(colorKey: String?, randomHexColorCode: String): String {
        var hexColorCode: String? = null

        colorKey?.let { colorKey ->
            withContext(dispatchers.io) {
                appContext.getSharedPreferences("sphinx_colors", Context.MODE_PRIVATE).let { sharedPrefs ->
                    sharedPrefs.getString(colorKey, null)?.let { savedHexCode ->
                        hexColorCode = savedHexCode
                    } ?: run {
                        hexColorCode = randomHexColorCode

                        sharedPrefs?.edit()?.let { editor ->
                            editor.putString(colorKey, randomHexColorCode).let { editor ->
                                if (!editor.commit()) {
                                    editor.apply()
                                }
                            }
                        }
                    }
                }
            }
        }
        return hexColorCode ?: randomHexColorCode
    }
}