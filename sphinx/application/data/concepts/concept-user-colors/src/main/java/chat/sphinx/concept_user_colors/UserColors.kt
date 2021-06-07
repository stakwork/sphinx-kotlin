package chat.sphinx.concept_user_colors

abstract class UserColors {
    abstract suspend fun getHexCodeForKey(colorKey: String?, randomHexCode: String): String
}