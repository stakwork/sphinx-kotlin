package chat.sphinx.web_view.ui

import android.animation.Animator
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import chat.sphinx.web_view.R
import chat.sphinx.web_view.databinding.FragmentWebViewBinding
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class WebViewFragment: SideEffectDetailFragment<
        FragmentActivity,
        WebViewSideEffect,
        WebViewViewState,
        WebViewViewModel,
        FragmentWebViewBinding
        >(R.layout.fragment_web_view)
{
    override val viewModel: WebViewViewModel by viewModels()
    override val binding: FragmentWebViewBinding by viewBinding(FragmentWebViewBinding::bind)

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            includeWebViewHeader.apply header@ {

                this@header.textViewDetailScreenHeaderNavBack.setOnClickListener {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.popBackStack()
                    }
                }

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
        }

        setupBoost()
        setupFragmentLayout()
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    private fun setupFragmentLayout() {
        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.layoutConstraintWebViewLayout)
    }

    private fun setupBoost() {
        binding.apply {
            includeLayoutBoostFireworks.apply {
                lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener{
                    override fun onAnimationStart(animation: Animator) {}

                    override fun onAnimationEnd(animation: Animator) {
                        root.gone
                    }

                    override fun onAnimationCancel(animation: Animator) {}

                    override fun onAnimationRepeat(animation: Animator) {}
                })
            }

            includeLayoutCustomBoost.apply {
                removeFocusOnEnter(editTextCustomBoost)

                imageViewFeedBoostButton.setOnClickListener {
                    val amount = editTextCustomBoost.text.toString()
                        .replace(" ", "")
                        .toLongOrNull()?.toSat() ?: Sat(0)

                    viewModel.sendBoost(
                        amount,
                        fireworksCallback = {
                            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                                setupBoostAnimation(null, amount)

                                includeLayoutBoostFireworks.apply fireworks@ {
                                    this@fireworks.root.visible
                                    this@fireworks.lottieAnimationView.playAnimation()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private suspend fun setupBoostAnimation(
        photoUrl: PhotoUrl?,
        amount: Sat?
    ) {
        binding.apply {
            includeLayoutCustomBoost.apply {
                editTextCustomBoost.setText(
                    (amount ?: Sat(100)).asFormattedString()
                )
            }

            includeLayoutBoostFireworks.apply {
                photoUrl?.let { photoUrl ->
                    imageLoader.load(
                        imageViewProfilePicture,
                        photoUrl.value,
                        imageLoaderOptions
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

    private fun loadWebView(url: String) {
        binding.apply {

            webView.settings.javaScriptEnabled = true
            webView.settings.loadWithOverviewMode = true
            webView.settings.useWideViewPort = true
            webView.settings.builtInZoomControls = true

            webView.loadUrl(url)
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url.toString()

                    if (url.contains("open=system")) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        view?.context?.startActivity(intent)
                        return true
                    }

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
        when (viewState) {
            is WebViewViewState.Idle -> {
            }

            is WebViewViewState.FeedDataLoaded -> {
                binding.apply {
                    includeWebViewHeader.apply {
                        textViewDetailScreenHeaderNavBack.goneIfFalse(viewState.fromArticlesList)
                        textViewDetailScreenHeaderName.text = viewState.viewTitle
                    }

                    includeLayoutCustomBoost.apply {
                        if (viewState.isFeedUrl) {
                            layoutConstraintBoostButtonContainer.visible
                            layoutConstraintBoostButtonContainer.alpha = if (viewState.feedHasDestinations) 1.0f else 0.3f
                            imageViewFeedBoostButton.isEnabled = viewState.feedHasDestinations
                            editTextCustomBoost.isEnabled = viewState.feedHasDestinations
                        } else {
                            layoutConstraintBoostButtonContainer.gone
                        }
                    }
                }

                setupBoostAnimation(
                    viewState.ownerPhotoUrl,
                    viewState.boostAmount
                )

                loadWebView(viewState.url)
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: WebViewSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
