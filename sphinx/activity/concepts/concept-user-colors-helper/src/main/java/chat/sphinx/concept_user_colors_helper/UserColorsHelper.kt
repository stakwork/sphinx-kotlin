package chat.sphinx.concept_user_colors_helper

abstract class UserColorsHelper {
    abstract suspend fun getHexCodeForKey(colorKey: String, randomHexColorCode: String): String
}