package chat.sphinx.chat_common.util

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import chat.sphinx.chat_common.R
import com.amrdeveloper.codeview.CodeView

class CodeSnippetLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayoutCompat(context, attrs, defStyle) {
//
//    val javaScript_code = "var horizontalScrollView = HorizontalScrollView(context)\n" +
//            "val textView = TextView(context)\n" +
//            "textView.text = \"Your Text Here\"\n" +
//            "val layoutParams = LinearLayout.LayoutParams(\n" +
//            "    LinearLayout.LayoutParams.WRAP_CONTENT,\n" +
//            "    LinearLayout.LayoutParams.WRAP_CONTENT\n" +
//            ")"


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // custom measurement logic using customAttribute
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // custom layout logic using customAttribute
        super.onLayout(changed, l, t, r, b)
    }


    fun addTextView(text: String) {
        val textView = AppCompatTextView(context)
        textView.setPadding(16, 10, 14, 10)
        textView.text = text

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        textView.setTextAppearance(R.style.chat_message_body_text)

        textView.layoutParams = layoutParams
        addView(textView)
    }

    fun addCodeSnippet(code: String) {
        val horizontalScrollView = HorizontalScrollView(context)
        val codeView = CodeView(context)
        val jetBrainsMono = ResourcesCompat.getFont(context, R.font.roboto_bold)
        codeView.setPadding(40)
        codeView.setText(code)
        codeView.typeface = jetBrainsMono
        codeView.textSize = 15F
        codeView.isEnabled = false
        codeView.setEnableAutoIndentation(true)
        codeView.setLineNumberTextColor(Color.GRAY)
        codeView.setLineNumberTextSize(30f)
        CodeSnippetLanguageManager.applyFiveColorsDarkTheme(context, codeView)

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        horizontalScrollView.layoutParams = layoutParams
        codeView.layoutParams = layoutParams
        horizontalScrollView.addView(codeView)
        horizontalScrollView.setPadding(16, 10, 14, 10)
        addView(horizontalScrollView)
    }
}