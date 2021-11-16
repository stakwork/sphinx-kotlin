package chat.sphinx.web_view.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.web_view.R
import chat.sphinx.web_view.databinding.FragmentWebViewBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import kotlinx.coroutines.launch

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
        }

        setupFragmentLayout()
    }

    fun setupFragmentLayout() {
        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.layoutConstraintWebViewLayout)
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
}
