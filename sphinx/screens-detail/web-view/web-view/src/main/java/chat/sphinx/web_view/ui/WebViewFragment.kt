package chat.sphinx.web_view.ui

import android.animation.Animator
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.web_view.R
import chat.sphinx.web_view.databinding.FragmentWebViewBinding
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class WebViewFragment: BaseFragment<
        WebViewViewState,
        WebViewViewModel,
        FragmentWebViewBinding
        >(R.layout.fragment_web_view)
{
    override val viewModel: WebViewViewModel by viewModels()
    override val binding: FragmentWebViewBinding by viewBinding(FragmentWebViewBinding::bind)

    private val args: WebViewFragmentArgs by navArgs()

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .build()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            includeWebViewHeader.apply header@ {
                
                this@header.textViewDetailScreenHeaderNavBack.goneIfFalse(args.argFromList)
                this@header.textViewDetailScreenHeaderNavBack.setOnClickListener {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.popBackStack()
                    }
                }

                this@header.textViewDetailScreenHeaderName.text = args.argTitle
                this@header.textViewDetailScreenClose.setOnClickListener {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.closeDetailScreen()
                    }
                }
            }

            webView.settings.javaScriptEnabled = true
            webView.settings.loadWithOverviewMode = true
            webView.settings.useWideViewPort = true
            webView.settings.builtInZoomControls = true

            removeFocusOnEnter(editTextCustomBoost)

            imageViewFeedBoostButton.setOnClickListener {
                val customAmount = editTextCustomBoost.text.toString().toLong().toSat()

                viewModel.sendBoost(
                    customAmount
                )

                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    setupBoostAnimation(null, customAmount)

                    binding.includeLayoutBoostFireworks.apply {
                        root.visible

                        lottieAnimationView.playAnimation()
                    }
                }
            }
        }

        setupBoost()
        setupFragmentLayout()
    }

    fun setupFragmentLayout() {
        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.layoutConstraintWebViewLayout)
    }

    private fun setupBoost() {
        binding.apply {
            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                viewModel.feedSharedFlow.collect { feed ->
                    feed?.let { nnFeed ->
                        layoutConstraintBoostButtonContainer.visible
                        layoutConstraintBoostButtonContainer.alpha = if (nnFeed.hasDestinations) 1.0f else 0.3f
                        imageViewFeedBoostButton.isEnabled = nnFeed.hasDestinations
                    } ?: run {
                        layoutConstraintBoostButtonContainer.gone
                    }
                }
            }

            includeLayoutBoostFireworks.apply {
                lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener{
                    override fun onAnimationEnd(animation: Animator?) {
                        root.gone
                    }

                    override fun onAnimationRepeat(animation: Animator?) {}

                    override fun onAnimationCancel(animation: Animator?) {}

                    override fun onAnimationStart(animation: Animator?) {}
                })
            }
        }
    }

    private suspend fun setupBoostAnimation(
        photoUrl: PhotoUrl?,
        amount: Sat?
    ) {
        binding.apply {
            editTextCustomBoost.let {
                it.setText(amount?.asFormattedString())
            }

            includeLayoutBoostFireworks.apply {

                photoUrl?.let { photoUrl ->
                    imageLoader.load(
                        imageViewProfilePicture,
                        photoUrl.value,
                        ImageLoaderOptions.Builder()
                            .placeholderResId(R.drawable.ic_profile_avatar_circle)
                            .transformation(Transformation.CircleCrop)
                            .build()
                    )
                }

                textViewSatsAmount.text = amount?.asFormattedString()
            }
        }
    }

    private fun removeFocusOnEnter(editText: EditText?) {
        editText?.setOnEditorActionListener(object:
            TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    editText.let { nnEditText ->
                        binding.root.context.inputMethodManager?.let { imm ->
                            if (imm.isActive(nnEditText)) {
                                imm.hideSoftInputFromWindow(nnEditText.windowToken, 0)
                                nnEditText.clearFocus()
                            }
                        }
                    }
                    return true
                }
                return false
            }
        })
    }

    override fun onResume() {
        super.onResume()

        loadWebView()
    }

    private fun loadWebView() {
        binding.apply {

            webView.settings.javaScriptEnabled = true
            webView.settings.loadWithOverviewMode = true
            webView.settings.useWideViewPort = true
            webView.settings.builtInZoomControls = true

            webView.loadUrl(args.argUrl)
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url.toString()
                    view?.loadUrl(url)
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    progressBarLoading.gone
                    super.onPageFinished(view, url)
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    progressBarLoading.gone
                    super.onReceivedError(view, request, error)
                }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: WebViewViewState) {
//        TODO("Not yet implemented")
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.boostAnimationViewStateContainer.collect { viewState ->
                when (viewState) {
                    is BoostAnimationViewState.Idle -> {
                    }

                    is BoostAnimationViewState.BoosAnimationInfo -> {
                        setupBoostAnimation(
                            viewState.photoUrl,
                            viewState.amount
                        )
                    }
                }
            }
        }
    }
}
