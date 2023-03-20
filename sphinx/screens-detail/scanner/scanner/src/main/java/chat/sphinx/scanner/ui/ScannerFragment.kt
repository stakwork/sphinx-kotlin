package chat.sphinx.scanner.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.scanner.R
import chat.sphinx.scanner.databinding.FragmentScannerBinding
import chat.sphinx.scanner.navigation.BackType
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

typealias BarcodeListener = (barcode: String) -> Unit

@AndroidEntryPoint
internal class ScannerFragment: SideEffectDetailFragment<
        Context,
        NotifySideEffect,
        ScannerViewState,
        ScannerViewModel,
        FragmentScannerBinding
        >(R.layout.fragment_scanner)
{
    override val binding: FragmentScannerBinding by viewBinding(FragmentScannerBinding::bind)
    override val viewModel: ScannerViewModel by viewModels()

    private val cameraExecutor: ExecutorService by lazy(LazyThreadSafetyMode.NONE) {
        Executors.newSingleThreadExecutor()
    }
    private val processingBarcode = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // have to call it here so it gets injected and can
        // catch the request asap
        viewModel
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.goBack(BackType.CloseDetailScreen)
        }
    }

    private val requestPermissionLauncher by lazy(LazyThreadSafetyMode.NONE) {
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startCamera()
            } else {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.submitSideEffect(
                        NotifySideEffect(getString(R.string.scanner_notify_permissions_needed))
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        (requireActivity() as InsetterActivity).addNavigationBarPadding(binding.root)

        binding.imageViewGallery.setOnClickListener {
           openGalleryToReadQr.launch("image/*")
        }

        binding.includeScannerHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.scanner_header_name)

            textViewDetailScreenClose.setOnClickListener {
                viewModel.goBack(BackType.CloseDetailScreen)
            }
            textViewDetailScreenHeaderNavBack.setOnClickListener {
                viewModel.goBack(BackType.PopBackStack)
            }
        }

        binding.buttonScannerSave.setOnClickListener {
            val input = binding.editTextCode.text?.toString()
            if (input != null && input.isNotEmpty()) {
                viewModel.processResponse(ScannerResponse(input.trim()))
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: ScannerViewState) {
        binding.apply {
            @Exhaustive
            when (viewState) {
                is ScannerViewState.LayoutVisibility -> {
                    includeScannerHeader.textViewDetailScreenHeaderNavBack.goneIfFalse(viewState.showBackButton)
                    layoutConstraintScannerInputContent.goneIfFalse(viewState.showBottomView)

                    editTextCode.hint = if (viewState.scannerModeLabel.isNotEmpty()) {
                        viewState.scannerModeLabel
                    } else {
                        getString(R.string.scanner_edit_text_hint)
                    }
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: NotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }

    private fun startCamera() {
        // Create an instance of the ProcessCameraProvider,
        // which will be used to bind the use cases to a lifecycle owner.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        // Add a listener to the cameraProviderFuture.
        // The first argument is a Runnable, which will be where the magic actually happens.
        // The second argument (way down below) is an Executor that runs on the main thread.
        cameraProviderFuture.addListener({
            // Add a ProcessCameraProvider, which binds the lifecycle of your camera to
            // the LifecycleOwner within the application's life.
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Initialize the Preview object, get a surface provider from your PreviewView,
            // and set it on the preview instance.
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRAnalyzer { barcode ->
                        if (processingBarcode.compareAndSet(false, true)) {
                            retrieveCode(barcode)
                        }
                    })
                }

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind any bound use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to lifecycleOwner
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
//                Log.e("PreviewUseCase", "Binding failed! :(", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun retrieveCode(code: String?) {
        if (code.isNullOrEmpty()) {
            return
        }

        viewModel.processResponse(ScannerResponse(code.trim()))
    }

    class QRAnalyzer(private val barcodeListener: BarcodeListener) : ImageAnalysis.Analyzer {
        // Get an instance of BarcodeScanner
        private val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )

        @androidx.camera.core.ExperimentalGetImage
        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                // Pass image to the scanner and have it do its thing
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        // Task completed successfully
                        for (barcode in barcodes) {
                            barcodeListener(barcode.rawValue ?: "")
                        }
                    }
                    .addOnFailureListener {
                        // You should really do something about Exceptions
                    }
                    .addOnCompleteListener {
                        // It's important to close the imageProxy
                        imageProxy.close()
                    }
            }
        }
    }

    private fun allPermissionsGranted() = arrayOf(Manifest.permission.CAMERA).all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val openGalleryToReadQr = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val image: InputImage
        try {
            uri?.let {
                image = InputImage.fromFilePath(requireContext(), it)

                val scanner = BarcodeScanning.getClient()
                scanner.process(image).addOnSuccessListener { barcodes ->
                    barcodes.forEach { barcode ->
                        val result = barcode.rawValue?: ""
                        retrieveCode(result)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}
