package chat.sphinx.resources

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.lang.Exception

@Serializable
data class WordLists(
    val ENGLISH: List<String>,
    val JAPANESE: List<String>,
    val KOREAN: List<String>,
    val SPANISH: List<String>,
    val SIMPLIFIED_CHINESE: List<String>,
    val TRADITIONAL_CHINESE: List<String>,
    val FRENCH: List<String>,
    val ITALIAN: List<String>
)

class MnemonicLanguagesUtils(private val context: Context) {

    enum class Language {
        ENGLISH,
        JAPANESE,
        KOREAN,
        SPANISH,
        SIMPLIFIED_CHINESE,
        TRADITIONAL_CHINESE,
        FRENCH,
        ITALIAN;
    }

    private fun loadJsonFromAsset(filename: String): String {
        val bufferedReader = context.assets.open(filename).bufferedReader()
        return bufferedReader.use { it.readText() }
    }

    private fun getWordsForLanguage(language: Language): List<String> {
        try {
            val jsonContent = loadJsonFromAsset("words-list-constants.json")
            val wordLists = Json.decodeFromString<WordLists>(jsonContent)

            return when (language) {
                Language.ENGLISH -> wordLists.ENGLISH
                Language.JAPANESE -> wordLists.JAPANESE
                Language.KOREAN -> wordLists.KOREAN
                Language.SPANISH -> wordLists.SPANISH
                Language.SIMPLIFIED_CHINESE -> wordLists.SIMPLIFIED_CHINESE
                Language.TRADITIONAL_CHINESE -> wordLists.TRADITIONAL_CHINESE
                Language.FRENCH -> wordLists.FRENCH
                Language.ITALIAN -> wordLists.ITALIAN
            }
        }
        catch (e: Exception) {
            return emptyList()
        }
    }

    private fun findLanguage(word: String): Language? {
        for (language in Language.values()) {
            if (getWordsForLanguage(language).contains(word)) {
                return language
            }
        }
        return null
    }

    fun validateWords(words: List<String>): Boolean {
        val language = findLanguage(words[0])

        if (language != null) {
            val wordsList = getWordsForLanguage(language)

            for (word in words) {
                if (!wordsList.contains(word)) {
                    return false
                }
            }
        } else {
            return false
        }

        return true
    }
}
