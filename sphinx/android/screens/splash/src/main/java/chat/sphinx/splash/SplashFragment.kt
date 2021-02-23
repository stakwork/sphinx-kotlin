package chat.sphinx.splash

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.Space
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.splash.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment: Fragment(R.layout.fragment_splash) {

    private val binding: FragmentSplashBinding by viewBinding(FragmentSplashBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSpaceHeight(binding.spaceSplashStatusBar)
    }

    /**
     * Have to programmatically set height of the space to the height of the status bar
     * for the device so the image view does not move at startup.
     * */
    private fun setSpaceHeight(space: Space) {
        requireActivity().apply {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val height = if (resourceId > 0) {
                resources.getDimensionPixelSize(resourceId)
            } else {
                Rect().apply {
                    window.decorView.getWindowVisibleDisplayFrame(this)
                }.top
            }

            val params = space.layoutParams.apply {
                this.height = height
            }
            space.layoutParams = params
        }
    }
}