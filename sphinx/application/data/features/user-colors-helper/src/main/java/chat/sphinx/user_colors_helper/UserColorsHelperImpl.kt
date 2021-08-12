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

    private val colors: MutableMap<String, String> = mutableMapOf()

    override suspend fun getHexCodeForKey(
        colorKey: String?,
        randomHexColorCode: String
    ): String {

        var colorHexCode: String? = null

        colorKey?.let { colorKey ->
            colors[colorKey]?.let { colorHexCode ->
                return colorHexCode
            }

            withContext(dispatchers.io) {
                appContext.getSharedPreferences("sphinx_colors", Context.MODE_PRIVATE).let { sharedPrefs ->
                    colorHexCode = sharedPrefs.getString(colorKey, null) ?: run {
                        sharedPrefs?.edit()?.let { editor ->
                            editor.putString(colorKey, randomHexColorCode).let { editor ->
                                if (!editor.commit()) {
                                    editor.apply()
                                }
                            }
                        }
                        randomHexColorCode
                    }
                }
            }

            colorHexCode?.let { nnColorHexCode ->
                colors.put(colorKey, nnColorHexCode)
            }
        }

        return colorHexCode ?: randomHexColorCode
    }
}