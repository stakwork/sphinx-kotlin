package chat.sphinx.chat_common.util

import android.content.Context
import androidx.core.content.ContextCompat
import chat.sphinx.chat_common.R
import com.amrdeveloper.codeview.Code
import com.amrdeveloper.codeview.CodeView
import com.amrdeveloper.codeview.Keyword
import java.util.ArrayList
import java.util.HashSet
import java.util.regex.Pattern


object CodeSnippetLanguageManager {
    private val PATTERN_KEYWORDS = Pattern.compile(
        "\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|false|final|finally|float|for|function|goto|if|implements|import|instanceof|int|interface|long|module|native|new|null|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|true|try|void|volatile|while|with|yield|__FILE__|__LINE__|__CLASS__|__METHOD__|and|as|async|await|await|assert|bool|break|case|catch|class|const|continue|debugger|default|delete|do|double|else|enum|val|export|extends|false|final|finally|float|for|from|function|get|go|goto|if|implements|import|in|include|instanceof|int|interface|is|let|long|module|native|new|null|of|open|package|private|protected|public|return|set|short|static|super|switch|synchronized|this|throw|throws|to|transient|true|try|type|typeof|undefined|union|unsigned|use|var|void|volatile|while|with|yield|companion|crossinline|data|dynamic|field|fileprivate|get|if|import|inout|internal|is|lateinit|let|operator|out|override|reified|return|sealed|set|super|tailrec|this|throw|try|when|where|while|ARRAY|BOOLEAN|CHAR|DOUBLE|INT|LONG|SHORT|Void|abstract|and|array|as|break|callable|case|catch|class|clone|const|continue|declare|default|die|do|echo|else|elseif|empty|enddeclare|endfor|endforeach|endif|endswitch|endwhile|eval|exit|extends|final|finally|fn|for|foreach|function|global|goto|if|implements|include|include_once|instanceof|insteadof|interface|isset|list|match|namespace|new|or|print|private|protected|public|require|require_once|return|static|switch|throw|trait|try|unset|use|var|while|with|xor|class|deinit|enum|extension|func|import|init|inout|let|operator|protocol|static|struct|subscript|typealias|var|break|case|continue|default|defer|do|else|enum|fallthrough|false|for|func|guard|if|in|init|inout|let|nil|repeat|return|self|static|struct|subscript|super|switch|true|try|typealias|unowned|var|weak|while)\\b"
    )
    private val PATTERN_BUILTINS = Pattern.compile("[,:;[->]{}()]")
    private val PATTERN_SINGLE_LINE_COMMENT = Pattern.compile("//[^\\n]*")
    private val PATTERN_MULTI_LINE_COMMENT = Pattern.compile("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/")
    private val PATTERN_ATTRIBUTE = Pattern.compile("\\.[a-zA-Z0-9_]+")
    private val PATTERN_OPERATION =
        Pattern.compile(":|==|>|<|!=|>=|<=|->|=|>|<|%|-|-=|%=|\\+|\\-|\\-=|\\+=|\\^|\\&|\\|::|\\?|\\*")
    private val PATTERN_GENERIC = Pattern.compile("<[a-zA-Z0-9,<>]+>")
    private val PATTERN_ANNOTATION = Pattern.compile("@.[a-zA-Z0-9]+")
    private val PATTERN_TODO_COMMENT = Pattern.compile("//TODO[^\n]*")
    private val PATTERN_NUMBERS = Pattern.compile("\\b(\\d*[.]?\\d+)\\b")
    private val PATTERN_CHAR = Pattern.compile("['](.*?)[']")
    private val PATTERN_STRING = Pattern.compile("[\"](.*?)[\"]")
    private val PATTERN_HEX = Pattern.compile("0x[0-9a-fA-F]+")

    fun applyMonokaiTheme(context: Context, codeView: CodeView) {
        codeView.resetSyntaxPatternList()
        codeView.resetHighlighter()
        val resources = context.resources

        //View Background

        codeView.setBackgroundColor(ContextCompat.getColor(context, R.color.monokia_pro_black))

        //Syntax Colors
        codeView.addSyntaxPattern(PATTERN_HEX, ContextCompat.getColor(context, R.color.black))
        codeView.addSyntaxPattern(PATTERN_CHAR, ContextCompat.getColor(context, R.color.monokia_pro_green))
        codeView.addSyntaxPattern(PATTERN_STRING, ContextCompat.getColor(context, R.color.monokia_pro_orange))
        codeView.addSyntaxPattern(PATTERN_NUMBERS, ContextCompat.getColor(context, R.color.monokia_pro_purple))
        codeView.addSyntaxPattern(PATTERN_KEYWORDS, ContextCompat.getColor(context, R.color.monokia_pro_pink))
        codeView.addSyntaxPattern(PATTERN_BUILTINS, ContextCompat.getColor(context, R.color.monokia_pro_white))
        codeView.addSyntaxPattern(
            PATTERN_SINGLE_LINE_COMMENT,
            ContextCompat.getColor(context, R.color.monokia_pro_grey)
        )
        codeView.addSyntaxPattern(
            PATTERN_MULTI_LINE_COMMENT,
            ContextCompat.getColor(context, R.color.monokia_pro_grey)
        )
        codeView.addSyntaxPattern(PATTERN_ANNOTATION, ContextCompat.getColor(context, R.color.monokia_pro_pink))
        codeView.addSyntaxPattern(PATTERN_ATTRIBUTE, ContextCompat.getColor(context, R.color.monokia_pro_sky))
        codeView.addSyntaxPattern(PATTERN_GENERIC, ContextCompat.getColor(context, R.color.monokia_pro_pink))
        codeView.addSyntaxPattern(PATTERN_OPERATION, ContextCompat.getColor(context, R.color.monokia_pro_pink))
        //Default Color
        codeView.setTextColor(ContextCompat.getColor(context, R.color.monokia_pro_white))
        codeView.addSyntaxPattern(PATTERN_TODO_COMMENT, ContextCompat.getColor(context, R.color.gold))
        codeView.reHighlightSyntax()
    }

    fun applyNoctisWhiteTheme(context: Context, codeView: CodeView) {
        codeView.resetSyntaxPatternList()
        codeView.resetHighlighter()
        val resources = context.resources

        //View Background
        codeView.setBackgroundColor(ContextCompat.getColor(context, R.color.noctis_white))

        //Syntax Colors
        codeView.addSyntaxPattern(PATTERN_HEX, ContextCompat.getColor(context, R.color.noctis_purple))
        codeView.addSyntaxPattern(PATTERN_CHAR, ContextCompat.getColor(context, R.color.noctis_green))
        codeView.addSyntaxPattern(PATTERN_STRING, ContextCompat.getColor(context, R.color.noctis_green))
        codeView.addSyntaxPattern(PATTERN_NUMBERS, ContextCompat.getColor(context, R.color.noctis_purple))
        codeView.addSyntaxPattern(PATTERN_KEYWORDS, ContextCompat.getColor(context, R.color.noctis_pink))
        codeView.addSyntaxPattern(PATTERN_BUILTINS, ContextCompat.getColor(context, R.color.noctis_dark_blue))
        codeView.addSyntaxPattern(
            PATTERN_SINGLE_LINE_COMMENT,
            ContextCompat.getColor(context, R.color.noctis_grey)
        )
        codeView.addSyntaxPattern(
            PATTERN_MULTI_LINE_COMMENT,
            ContextCompat.getColor(context, R.color.noctis_grey)
        )
        codeView.addSyntaxPattern(PATTERN_ANNOTATION, ContextCompat.getColor(context, R.color.monokia_pro_pink))
        codeView.addSyntaxPattern(PATTERN_ATTRIBUTE, ContextCompat.getColor(context, R.color.noctis_blue))
        codeView.addSyntaxPattern(PATTERN_GENERIC, ContextCompat.getColor(context, R.color.monokia_pro_pink))
        codeView.addSyntaxPattern(PATTERN_OPERATION, ContextCompat.getColor(context, R.color.monokia_pro_pink))

        //Default Color
        codeView.setTextColor(ContextCompat.getColor(context, R.color.noctis_orange))
        codeView.addSyntaxPattern(PATTERN_TODO_COMMENT, ContextCompat.getColor(context, R.color.gold))
        codeView.reHighlightSyntax()
    }

    fun applyFiveColorsDarkTheme(context: Context, codeView: CodeView) {
        codeView.resetSyntaxPatternList()
        codeView.resetHighlighter()
        val resources = context.resources

        //View Background
        codeView.setBackgroundColor(ContextCompat.getColor(context, R.color.five_dark_black))

        //Syntax Colors
        codeView.addSyntaxPattern(PATTERN_HEX, ContextCompat.getColor(context, R.color.five_dark_purple))
        codeView.addSyntaxPattern(PATTERN_CHAR, ContextCompat.getColor(context, R.color.five_dark_yellow))
        codeView.addSyntaxPattern(PATTERN_STRING, ContextCompat.getColor(context, R.color.five_dark_yellow))
        codeView.addSyntaxPattern(PATTERN_NUMBERS, ContextCompat.getColor(context, R.color.five_dark_purple))
        codeView.addSyntaxPattern(PATTERN_KEYWORDS, ContextCompat.getColor(context, R.color.five_dark_purple))
        codeView.addSyntaxPattern(PATTERN_BUILTINS, ContextCompat.getColor(context, R.color.five_dark_white))
        codeView.addSyntaxPattern(
            PATTERN_SINGLE_LINE_COMMENT,
            ContextCompat.getColor(context, R.color.five_dark_grey)
        )
        codeView.addSyntaxPattern(
            PATTERN_MULTI_LINE_COMMENT,
            ContextCompat.getColor(context, R.color.five_dark_grey)
        )
        codeView.addSyntaxPattern(PATTERN_ANNOTATION, ContextCompat.getColor(context, R.color.five_dark_purple))
        codeView.addSyntaxPattern(PATTERN_ATTRIBUTE, ContextCompat.getColor(context, R.color.five_dark_blue))
        codeView.addSyntaxPattern(PATTERN_GENERIC, ContextCompat.getColor(context, R.color.five_dark_purple))
        codeView.addSyntaxPattern(PATTERN_OPERATION, ContextCompat.getColor(context, R.color.five_dark_purple))

        //Default Color
        codeView.setTextColor(ContextCompat.getColor(context, R.color.five_dark_white))
        codeView.addSyntaxPattern(PATTERN_TODO_COMMENT, ContextCompat.getColor(context, R.color.gold))
        codeView.reHighlightSyntax()
    }

    fun applyOrangeBoxTheme(context: Context, codeView: CodeView) {
        codeView.resetSyntaxPatternList()
        codeView.resetHighlighter()
        val resources = context.resources

        //View Background
        codeView.setBackgroundColor(ContextCompat.getColor(context, R.color.orange_box_black))

        //Syntax Colors
        codeView.addSyntaxPattern(PATTERN_HEX, ContextCompat.getColor(context, R.color.gold))
        codeView.addSyntaxPattern(PATTERN_CHAR, ContextCompat.getColor(context, R.color.orange_box_orange2))
        codeView.addSyntaxPattern(PATTERN_STRING, ContextCompat.getColor(context, R.color.orange_box_orange2))
        codeView.addSyntaxPattern(PATTERN_NUMBERS, ContextCompat.getColor(context, R.color.five_dark_purple))
        codeView.addSyntaxPattern(PATTERN_KEYWORDS, ContextCompat.getColor(context, R.color.orange_box_orange1))
        codeView.addSyntaxPattern(PATTERN_BUILTINS, ContextCompat.getColor(context, R.color.orange_box_grey))
        codeView.addSyntaxPattern(
            PATTERN_SINGLE_LINE_COMMENT,
            ContextCompat.getColor(context, R.color.orange_box_dark_grey)
        )
        codeView.addSyntaxPattern(
            PATTERN_MULTI_LINE_COMMENT,
            ContextCompat.getColor(context, R.color.orange_box_dark_grey)
        )
        codeView.addSyntaxPattern(
            PATTERN_ANNOTATION,
            ContextCompat.getColor(context, R.color.orange_box_orange1)
        )
        codeView.addSyntaxPattern(PATTERN_ATTRIBUTE, ContextCompat.getColor(context, R.color.orange_box_orange3))
        codeView.addSyntaxPattern(PATTERN_GENERIC, ContextCompat.getColor(context, R.color.orange_box_orange1))
        codeView.addSyntaxPattern(PATTERN_OPERATION, ContextCompat.getColor(context, R.color.gold))

        //Default Color
        codeView.setTextColor(ContextCompat.getColor(context, R.color.five_dark_white))
        codeView.addSyntaxPattern(PATTERN_TODO_COMMENT, ContextCompat.getColor(context, R.color.gold))
        codeView.reHighlightSyntax()
    }
}