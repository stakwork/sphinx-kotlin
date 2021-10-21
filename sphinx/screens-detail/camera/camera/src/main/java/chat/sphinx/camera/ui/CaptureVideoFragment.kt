package chat.sphinx.camera.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.camera.R
import chat.sphinx.camera.databinding.FragmentCaptureVideoBinding
import chat.sphinx.camera.model.CameraItem
import chat.sphinx.camera.ui.viewstate.CameraViewState
import chat.sphinx.camera.ui.viewstate.CapturePreviewViewState
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import com.example.android.camera.utils.OrientationLiveData
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
internal class CaptureVideoFragment: SideEffectFragment<
        FragmentActivity,
        CameraSideEffect,
        CameraViewState,
        CameraViewModel,
        FragmentCaptureVideoBinding,
        >(R.layout.fragment_camera)
{
    @Suppress("PrivatePropertyName")
    private val PERMISSIONS_REQUIRED = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    )

    private var _binding: FragmentCaptureVideoBinding? = null
    override val binding: FragmentCaptureVideoBinding
        get() = _binding!!

    override val viewModel: CameraViewModel by viewModels()

    private lateinit var videoCapture: VideoCapture<Recorder>
    private var activeRecording: ActiveRecording? = null
    private lateinit var recordingState: VideoRecordEvent

    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(requireContext()) }

    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status)
            recordingState = event

        updateUI(event)

        if (event is VideoRecordEvent.Finalize) {
            event.outputResults.outputUri.path?.let {
                lifecycleScope.launch(viewModel.io) {
                    val file = File(it)
                    viewModel.updateMediaPreviewViewState(
                        CapturePreviewViewState.Preview.VideoPreview(file)
                    )
                }

            }
        }
    }

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    private val requestPermissionLauncher by lazy(LazyThreadSafetyMode.NONE) {
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { response ->

            try {
                for (permission in PERMISSIONS_REQUIRED) {
                    if (response[permission] != true) {
                        throw Exception()
                    }
                }

                if (currentViewState !is CameraViewState.Active) {
                    viewModel.updateViewState(
                        CameraViewState.Active.BackCamera(viewModel.getBackCamera())
                    )
                }
            } catch (e: Exception) {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.submitSideEffect(
                        CameraSideEffect.Notify(getString(R.string.camera_permissions_required))
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel
    }

    @Volatile
    private var orientationLiveData: OrientationLiveData? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCaptureVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.includeCameraFooter.root)
            .addNavigationBarPadding(binding.includeCameraImagePreview.layoutConstraintCameraImagePreviewFooter)

        if (!hasPermissions(requireContext())) {
            requestPermissionLauncher.launch(PERMISSIONS_REQUIRED)
        } else {
            if (currentViewState !is CameraViewState.Active) {
                viewModel.updateViewState(
                    CameraViewState.Active.BackCamera(viewModel.getBackCamera())
                )
            }
        }

        binding.includeCameraFooter.imageViewCameraFooterBackFront.setOnClickListener {
            @Exhaustive
            when (currentViewState) {
                is CameraViewState.Active.BackCamera -> {
                    viewModel.updateViewState(
                        CameraViewState.Active.FrontCamera(viewModel.getFrontCamera())
                    )
                }
                null,
                is CameraViewState.Idle,
                is CameraViewState.Active.FrontCamera -> {
                    viewModel.updateViewState(
                        CameraViewState.Active.BackCamera(viewModel.getBackCamera())
                    )
                }
            }
        }

        binding.includeCameraImagePreview.apply {
            imageViewCameraImagePreview.setOnClickListener {
                viewModel
            }
            textViewCameraImagePreviewRetake.setOnClickListener {
                @Exhaustive
                when (val vs = viewModel.currentCapturePreviewViewState) {
                    is CapturePreviewViewState.None -> {}
                    is CapturePreviewViewState.Preview.ImagePreview -> {
                        viewModel.deleteImage(vs.media)
                        viewModel.updateMediaPreviewViewState(CapturePreviewViewState.None)
                    }
                    is CapturePreviewViewState.Preview.VideoPreview -> {
                        viewModel.deleteImage(vs.media)
                        viewModel.updateMediaPreviewViewState(CapturePreviewViewState.None)
                    }
                }
            }
            textViewCameraImagePreviewUse.setOnClickListener {
                @Exhaustive
                when (val vs = viewModel.currentCapturePreviewViewState) {
                    is CapturePreviewViewState.None -> {}
                    is CapturePreviewViewState.Preview -> {
                        textViewCameraImagePreviewRetake.isEnabled = false
                        viewModel.processSuccessfulResponse(vs)
                    }
                }
            }
        }

        binding.includeCameraFooter.textViewCameraFooterSwitch.text = "Take Picture"
        binding.includeCameraFooter.textViewCameraFooterSwitch.setOnClickListener {
            viewModel.goToCapturePictureFragment()
        }
    }

    /**
     * UpdateUI according to CameraX VideoRecordEvent type:
     *   - user starts capture.
     *   - this app disables all UI selections.
     *   - this app enables capture run-time UI (pause/resume/stop).
     *   - user controls recording with run-time UI, eventually tap "stop" to end.
     *   - this app informs CameraX recording to stop with recording.stop() (or recording.close()).
     *   - CameraX notify this app that the recording is indeed stopped, with the Finalize event.
     *   - this app starts VideoViewer fragment to view the captured result.
     */
    private fun updateUI(event: VideoRecordEvent) {
        lifecycleScope.launch(viewModel.mainImmediate) {
            binding.apply {
                when (event) {
                    is VideoRecordEvent.Start -> {
                        includeCameraFooter.imageViewCameraFooterShutter.gone
                        includeCameraFooter.imageViewCameraStop.visible
                    }
                    is VideoRecordEvent.Finalize-> {
                        includeCameraFooter.imageViewCameraStop.gone
                        includeCameraFooter.imageViewCameraFooterShutter.visible
                    }
                    else -> {
                        return@launch
                    }
                }
            }

        }
    }

    override fun onStop() {
        super.onStop()
        try {
            camera?.close()
        } catch (e: Throwable) {}
        camera = null
    }

    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    @Volatile
    private var camera: CameraDevice? = null

    private fun startCamera(cameraItem: CameraItem) {
        lifecycleScope.launch(viewModel.mainImmediate) {
            val cameraProvider = ProcessCameraProvider.getInstance(requireContext()).await()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(cameraItem.lensFacing.toInt())
                .build()

            val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build().apply {
                    setSurfaceProvider(binding.previewViewCamera.surfaceProvider)
                }

            // build a recorder, which can:
            //   - record video/audio to MediaStore(only use here), File, ParcelFileDescriptor
            //   - be used create recording(s) (the recording performs recording)
            val recorder = Recorder.Builder().build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    requireParentFragment(),
                    cameraSelector,
                    videoCapture,
                    preview
                )
            } catch (exc: Exception) {
                resetUIandState()
            }

            binding.includeCameraFooter.imageViewCameraFooterShutter.setOnClickListener { view ->
                view.gone

                lifecycleScope.launch(viewModel.io) {
                    if (activeRecording == null || recordingState is VideoRecordEvent.Finalize) {
                        binding.includeCameraFooter.imageViewCameraFooterShutter.gone
                        binding.includeCameraFooter.imageViewCameraStop.visible

                        startRecording()
                    }
                }
            }

            binding.includeCameraFooter.imageViewCameraStop.setOnClickListener { view ->
                view.gone
                lifecycleScope.launch(viewModel.io) {
                    if (activeRecording != null && recordingState !is VideoRecordEvent.Finalize) {
                        activeRecording?.stop()
                        activeRecording = null
                        delay(200L)
                    }
                }
            }
            // re-enable button to switch between back/front camera
            binding.includeCameraFooter.imageViewCameraFooterBackFront.isEnabled = true
        }
    }

    /**
     * ResetUI (restart):
     *    in case binding failed, let's give it another change for re-try. In future cases
     *    we might fail and user get notified on the status
     */
    private fun resetUIandState() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            binding.includeCameraFooter.imageViewCameraFooterShutter.visible
            binding.includeCameraFooter.imageViewCameraStop.gone

            // TODO: Prompt user of reset
        }
    }

    /**
     * Kick start the video recording
     *   - config Recorder to capture to MediaStoreOutput
     *   - register RecordEvent Listener
     *   - apply audio request from user
     *   - start recording!
     * After this function, user could start/pause/resume/stop recording and application listens
     * to VideoRecordEvent for the current recording status.
     */
    @SuppressLint("MissingPermission")
    private fun startRecording() {
        // create MediaStoreOutputOptions for our recorder: resulting our recording!
        val fileOutputOptions = FileOutputOptions.Builder(
            viewModel.createFile("mp4", false)
        ).build()

        // configure Recorder and Start recording to the mediaStoreOutput.
        activeRecording = videoCapture.output.prepareRecording(requireActivity(), fileOutputOptions)
                .withEventListener(
                    mainThreadExecutor,
                    captureListener
                )
                .withAudioEnabled()
                .start()
    }

    override suspend fun onSideEffectCollect(sideEffect: CameraSideEffect) {
        sideEffect.execute(requireActivity())
    }

    private val orientationObserver = Observer<Int> { orientation ->

    }

    override suspend fun onViewStateFlowCollect(viewState: CameraViewState) {

        binding.includeCameraFooter.imageViewCameraFooterShutter.setOnClickListener(null)
        binding.includeCameraFooter.imageViewCameraStop.setOnClickListener(null)

        @Exhaustive
        when (viewState) {
            is CameraViewState.Idle -> {}
            is CameraViewState.Active -> {
                viewState.cameraItem?.let { item ->

                    // disable button to switch between back/front camera
                    binding.includeCameraFooter.imageViewCameraFooterBackFront.isEnabled = false

                    try {
                        orientationLiveData?.removeObserver(orientationObserver)
                        orientationLiveData = OrientationLiveData(binding.root.context, item.characteristics).apply {
                            observe(viewLifecycleOwner, orientationObserver)
                        }

                        startCamera(item)
                    } catch (e: Exception) {}
                } // TODO: handle null case with no camera available view
            }
        }
    }

    // have to override here to always push
    // results to onViewStateFlowCollect w/o
    // comparing to currentViewState
    override fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->
                currentViewState = viewState
                onViewStateFlowCollect(viewState)
            }
        }
        onStopSupervisor.scope.launch(viewModel.io) {
            viewModel.collectImagePreviewViewState { viewState ->
                binding.includeCameraImagePreview.apply {
                    @Exhaustive
                    when (viewState) {
                        is CapturePreviewViewState.Preview.VideoPreview -> {
                            if (viewState.media.exists()) {
                                root.visible
                                // TODO: Load Video thumbnail+playback
//                                imageLoader.load(imageViewCameraImagePreview, viewState.media)
                            } else {
                                viewModel.updateMediaPreviewViewState(CapturePreviewViewState.None)
                            }
                        }
                        is CapturePreviewViewState.Preview.ImagePreview -> {
                            if (viewState.media.exists()) {
                                root.visible
                                imageLoader.load(imageViewCameraImagePreview, viewState.media)
                            } else {
                                viewModel.updateMediaPreviewViewState(CapturePreviewViewState.None)
                            }
                        }
                        is CapturePreviewViewState.None -> {
                            root.gone
                            imageViewCameraImagePreview.setImageDrawable(null)
                        }
                    }
                }
            }
        }
    }
}
