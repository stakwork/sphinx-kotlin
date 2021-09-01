package chat.sphinx.subscription.ui.widgets

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.RadioButton
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import chat.sphinx.subscription.ui.widgets.SphinxRadioGroup

class SphinxRadioGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {
    /**
     *
     * Returns the identifier of the selected radio button in this group.
     * Upon empty selection, the returned value is -1.
     *
     * @return the unique id of the selected radio button in this group
     *
     * @see .check
     * @see .clearCheck
     * @attr ref android.R.styleable#RadioGroup_checkedButton
     */
    // holds the checked id; the selection is empty by default
    @get:IdRes
    var checkedRadioButtonId = -1
        private set

    // tracks children radio buttons checked state
    private var mChildOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null

    // when true, mOnCheckedChangeListener discards events
    private var mProtectFromCheckedChange = false
    private var mOnCheckedChangeListener: OnCheckedChangeListener? = null
    private var mPassThroughListener: PassThroughHierarchyChangeListener? = null

    // Indicates whether the child was set from resources or dynamically, so it can be used
    // to sanitize autofill requests.
    private fun init() {
        mChildOnCheckedChangeListener = CheckedStateTracker()
        mPassThroughListener = PassThroughHierarchyChangeListener()
        super.setOnHierarchyChangeListener(mPassThroughListener)
    }

    /**
     * {@inheritDoc}
     */
    override fun setOnHierarchyChangeListener(listener: OnHierarchyChangeListener) {
        // the user listener is delegated to our pass-through listener
        mPassThroughListener!!.mOnHierarchyChangeListener = listener
    }

    /**
     * {@inheritDoc}
     */
    override fun onFinishInflate() {
        super.onFinishInflate()

        // checks the appropriate radio button as requested in the XML file
        if (checkedRadioButtonId != -1) {
            mProtectFromCheckedChange = true
            setCheckedStateForView(checkedRadioButtonId, true)
            mProtectFromCheckedChange = false
            setCheckedId(checkedRadioButtonId)
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (child is RadioButton) {
            val button = child
            if (button.isChecked) {
                mProtectFromCheckedChange = true
                if (checkedRadioButtonId != -1) {
                    setCheckedStateForView(checkedRadioButtonId, false)
                }
                mProtectFromCheckedChange = false
                setCheckedId(button.id)
            }
        }
        super.addView(child, index, params)
    }

    /**
     *
     * Sets the selection to the radio button whose identifier is passed in
     * parameter. Using -1 as the selection identifier clears the selection;
     * such an operation is equivalent to invoking [.clearCheck].
     *
     * @param id the unique id of the radio button to select in this group
     *
     * @see .getCheckedRadioButtonId
     * @see .clearCheck
     */
    fun check(@IdRes id: Int) {
        // don't even bother
        if (id != -1 && id == checkedRadioButtonId) {
            return
        }
        if (checkedRadioButtonId != -1) {
            setCheckedStateForView(checkedRadioButtonId, false)
        }
        if (id != -1) {
            setCheckedStateForView(id, true)
        }
        setCheckedId(id)
    }

    private fun setCheckedId(@IdRes id: Int) {
        checkedRadioButtonId = id
        if (mOnCheckedChangeListener != null) {
            mOnCheckedChangeListener!!.onCheckedChanged(this, checkedRadioButtonId)
        }
    }

    private fun setCheckedStateForView(viewId: Int, checked: Boolean) {
        val checkedView = findViewById<View>(viewId)
        if (checkedView != null && checkedView is RadioButton) {
            checkedView.isChecked = checked
        }
    }

    /**
     *
     * Clears the selection. When the selection is cleared, no radio button
     * in this group is selected and [.getCheckedRadioButtonId] returns
     * null.
     *
     * @see .check
     * @see .getCheckedRadioButtonId
     */
    fun clearCheck() {
        check(-1)
    }

    /**
     *
     * Register a callback to be invoked when the checked radio button
     * changes in this group.
     *
     * @param listener the callback to call on checked state change
     */
    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        mOnCheckedChangeListener = listener
    }

    override fun getAccessibilityClassName(): CharSequence {
        return this::class.java.name
    }

    /**
     *
     * Interface definition for a callback to be invoked when the checked
     * radio button changed in this group.
     */
    interface OnCheckedChangeListener {
        /**
         *
         * Called when the checked radio button has changed. When the
         * selection is cleared, checkedId is -1.
         *
         * @param group the group in which the checked radio button has changed
         * @param checkedId the unique identifier of the newly checked radio button
         */
        fun onCheckedChanged(group: SphinxRadioGroup?, @IdRes checkedId: Int)
    }

    private abstract inner class AbstractCheckedStateTracker : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            // prevents from infinite recursion
            if (mProtectFromCheckedChange) {
                return
            }
            mProtectFromCheckedChange = true
            if (checkedRadioButtonId != -1) {
                setCheckedStateForView(checkedRadioButtonId, false)
            }
            mProtectFromCheckedChange = false
            val id = buttonView.id
            setCheckedId(id)
        }
    }

    private inner class CheckedStateTracker : AbstractCheckedStateTracker(){
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            super.onCheckedChanged(buttonView, isChecked)

            if (isChecked) {
                // Hide soft keyboard
                context?.let {
                    val inputMethodManager = ContextCompat.getSystemService(it, InputMethodManager::class.java)
                    inputMethodManager?.hideSoftInputFromWindow(buttonView.windowToken, 0)
                }
            }
        }
    }

    private inner class CheckedStateWithEditTextTracker(val editText: EditText) : AbstractCheckedStateTracker() {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            super.onCheckedChanged(buttonView, isChecked)

            editText.isEnabled = isChecked

            if (editText.isEnabled && buttonView.isPressed) {
                editText.requestFocus()
                context?.let {
                    val inputMethodManager = ContextCompat.getSystemService(it, InputMethodManager::class.java)
                    inputMethodManager?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                }
            } else {
                editText.setText("")
            }
        }
    }

    private inner class CheckedStateWithDatePickerTracker(val editText: EditText) : AbstractCheckedStateTracker() {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            super.onCheckedChanged(buttonView, isChecked)

            editText.isEnabled = isChecked

            if (editText.isEnabled && buttonView.isPressed) {
                editText.requestFocus()
                editText.callOnClick()
            } else {
                editText.setText("")
            }
        }
    }

    /**
     *
     * A pass-through listener acts upon the events and dispatches them
     * to another listener. This allows the table layout to set its own internal
     * hierarchy change listener without preventing the user to setup his.
     */
    private inner class PassThroughHierarchyChangeListener : OnHierarchyChangeListener {
        var mOnHierarchyChangeListener: OnHierarchyChangeListener? = null

        /**
         * {@inheritDoc}
         */
        override fun onChildViewAdded(parent: View, child: View) {
            if (parent === this@SphinxRadioGroup && child is RadioButton) {
                var id = child.getId()
                // generates an id if it's missing
                if (id == NO_ID) {
                    id = generateViewId()
                    child.setId(id)
                }
                child.setOnCheckedChangeListener(
                    mChildOnCheckedChangeListener
                )
            }
            mOnHierarchyChangeListener?.onChildViewAdded(parent, child)
        }

        /**
         * {@inheritDoc}
         */
        override fun onChildViewRemoved(parent: View, child: View) {
            if (parent === this@SphinxRadioGroup && child is RadioButton) {
                child.setOnCheckedChangeListener(null)
            }
            mOnHierarchyChangeListener?.onChildViewRemoved(parent, child)
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.collectionInfo = AccessibilityNodeInfo.CollectionInfo.obtain(
            visibleChildWithTextCount,
            1, false,
            AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_SINGLE
        )
    }

    private val visibleChildWithTextCount: Int
        get() {
            var count = 0
            for (i in 0 until childCount) {
                if (getChildAt(i) is RadioButton) {
                    if (isVisibleWithText(getChildAt(i) as RadioButton)) {
                        count++
                    }
                }
            }
            return count
        }

    fun getIndexWithinVisibleButtons(child: View?): Int {
        if (child !is RadioButton) {
            return -1
        }
        var index = 0
        for (i in 0 until childCount) {
            if (getChildAt(i) is RadioButton) {
                val button = getChildAt(i) as RadioButton
                if (button === child) {
                    return index
                }
                if (isVisibleWithText(button)) {
                    index++
                }
            }
        }
        return -1
    }

    private fun isVisibleWithText(button: RadioButton): Boolean {
        return button.visibility == VISIBLE && !TextUtils.isEmpty(button.text)
    }

    /**
     * {@inheritDoc}
     */
    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        init()
    }

    fun setOnCheckedChangeListenerWithInputInteraction(radioButton: RadioButton, editText: EditText) {
        radioButton.setOnCheckedChangeListener(CheckedStateWithEditTextTracker(editText))
    }

    fun setOnCheckedChangeListenerWithDatePickerInputInteraction(radioButton: RadioButton, editText: EditText) {
        radioButton.setOnCheckedChangeListener(CheckedStateWithDatePickerTracker(editText))
    }
}