package chat.sphinx.splash.ui

import android.text.InputFilter
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import chat.sphinx.splash.R
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardLayoutViewState: ViewState<OnBoardLayoutViewState>() {
    abstract fun setInfoText(textView: TextView)
    abstract fun setScannerButton(viewModel: SplashViewModel, imageButton: ImageButton)
    abstract fun setEditTextInput(viewModel: SplashViewModel, editText: EditText)

    object Hidden: OnBoardLayoutViewState() {
        override fun setInfoText(textView: TextView) {
            textView.text = textView.context.getString(R.string.on_board_welcome_info_code)
        }

        override fun setScannerButton(viewModel: SplashViewModel, imageButton: ImageButton) {
            imageButton.isEnabled = false
            imageButton.setOnClickListener(null)
        }

        override fun setEditTextInput(viewModel: SplashViewModel, editText: EditText) {
            editText.isEnabled = false
            editText.setOnEditorActionListener(null)
        }
    }

    object InputCode: OnBoardLayoutViewState() {
        override fun setInfoText(textView: TextView) {
            textView.text = textView.context.getString(R.string.on_board_welcome_info_code)
        }

        override fun setScannerButton(viewModel: SplashViewModel, imageButton: ImageButton) {
            imageButton.isEnabled = true
            imageButton.visibility = View.VISIBLE
            imageButton.setOnClickListener {
                viewModel.navigateToScanner()
            }
        }

        override fun setEditTextInput(viewModel: SplashViewModel, editText: EditText) {
            editText.isEnabled = true
            editText.hint =
                editText.context.getString(R.string.on_board_edit_text_hint_code)

            editText.filters = arrayOf(InputFilter.LengthFilter(10_000))
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT

            editText.setOnEditorActionListener { _, actionId: Int, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    viewModel.processUserInput(
                        editText.text?.toString()
                    )
                    editText.setText("")
                    true
                } else {
                    false
                }
            }
        }
    }

    class DecryptKeys(val toDecrypt: ByteArray): OnBoardLayoutViewState() {
        override fun setInfoText(textView: TextView) {
            textView.text = textView.context.getString(R.string.on_board_welcome_info_decrypt)
        }

        override fun setScannerButton(viewModel: SplashViewModel, imageButton: ImageButton) {
            imageButton.visibility = View.GONE
            imageButton.isEnabled = false
            imageButton.setOnClickListener(null)
        }

        override fun setEditTextInput(viewModel: SplashViewModel, editText: EditText) {
            editText.isEnabled = true
            editText.hint = editText.context.getString(R.string.on_board_edit_text_hint_decrypt)

            editText.filters = arrayOf(InputFilter.LengthFilter(6))
            editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER

            editText.setOnEditorActionListener { _, actionId: Int, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    viewModel.decryptInput(
                        this,
                        editText.text?.toString()
                    )
                    editText.setText("")
                    true
                } else {
                    false
                }
            }
        }
    }
}
