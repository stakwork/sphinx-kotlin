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
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.RotationProvider
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.camera.R
import chat.sphinx.camera.databinding.FragmentCameraBinding
import chat.sphinx.camera.model.CameraItem
import chat.sphinx.camera.model.LensFacing
import chat.sphinx.camera.ui.viewstate.CameraViewState
import chat.sphinx.camera.ui.viewstate.CapturePreviewViewState
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject


@AndroidEntryPoint
internal class CameraFragment: SideEffectDetailFragment<
        FragmentActivity,
        CameraSideEffect,
        CameraViewState,
        CameraViewModel,
        FragmentCameraBinding,
        >(R.layout.fragment_camera)
{
    @Suppress("PrivatePropertyName")
    private val PERMISSIONS_REQUIRED = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    )

    override val binding: FragmentCameraBinding by viewBinding(FragmentCameraBinding::bind)

    override val viewModel: CameraViewModel by viewModels()

    @Volatile
    private var rotationProvider: RotationProvider? = null
    private var lastRotation: Int? = null
    @SuppressLint("RestrictedApi")
    private val rotationListener = { rotation: Int  ->
        lastRotation = rotation
        imageCapture.targetRotation = rotation
        videoCapture.targetRotation = rotation
    }

    private lateinit var imageCapture: ImageCapture
    private lateinit var videoCapture: VideoCapture<Recorder>

    private var activeRecording: ActiveRecording? = null
    private lateinit var recordingState: VideoRecordEvent

    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(requireContext()) }
    private lateinit var cameraExecutor: ExecutorService

    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status)
            recordingState = event

        updateUI(event)

        if (event is VideoRecordEvent.Finalize) {
            event.outputResults.outputUri.path?.let {
                lifecycleScope.launch(viewModel.io) {
                    viewModel.updateMediaPreviewViewState(
                        CapturePreviewViewState.Preview.VideoPreview(File(it))
                    )
                }
            }
        }
    }

    private val imageSavedCallback = object : ImageCapture.OnImageSavedCallback {
        override fun onError(exc: ImageCaptureException) {
            // TODO: Give user feedback on failure
        }

        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            lifecycleScope.launch(viewModel.io) {
                output.savedUri?.let { photoUri ->
                    viewModel.updateMediaPreviewViewState(
                        CapturePreviewViewState.Preview.ImagePreview(
                            photoUri.toFile()
                        )
                    )
                    delay(200L)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.includeCameraFooter.root)
            .addNavigationBarPadding(binding.includeCameraMediaPreview.layoutConstraintCameraMediaPreviewFooter)

        if (!hasPermissions(requireContext())) {
            requestPermissionLauncher.launch(PERMISSIONS_REQUIRED)
        } else {
            if (currentViewState !is CameraViewState.Active) {
                viewModel.updateViewState(
                    CameraViewState.Active.BackCamera(viewModel.getBackCamera())
                )
            }
        }
        rotationProvider = RotationProvider(binding.root.context)

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

        binding.includeCameraMediaPreview.apply {
            imageViewCameraImagePreview.setOnClickListener {
                viewModel
            }
            textViewCameraMediaPreviewRetake.setOnClickListener {
                @Exhaustive
                when (val vs = viewModel.currentCapturePreviewViewState) {
                    is CapturePreviewViewState.None -> {}
                    is CapturePreviewViewState.Preview.ImagePreview -> {
                        viewModel.deleteMedia(vs.media)
                        viewModel.updateMediaPreviewViewState(CapturePreviewViewState.None)
                    }
                    is CapturePreviewViewState.Preview.VideoPreview -> {
                        viewModel.deleteMedia(vs.media)
                        viewModel.updateMediaPreviewViewState(CapturePreviewViewState.None)
                    }
                }
            }
            textViewCameraMediaPreviewUse.setOnClickListener {
                @Exhaustive
                when (val vs = viewModel.currentCapturePreviewViewState) {
                    is CapturePreviewViewState.None -> {}
                    is CapturePreviewViewState.Preview -> {
                        textViewCameraMediaPreviewRetake.isEnabled = false
                        viewModel.processSuccessfulResponse(vs)
                    }
                }
            }
        }

        binding.includeCameraFooter.textViewCameraFooterCancel.setOnClickListener {
            viewModel.processCancellationResponse()
        }
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.processCancellationResponse()
        }
    }

    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

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

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio())
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(lastRotation ?: binding.previewViewCamera.display.rotation)
                .build()

            val recorder = Recorder.Builder().build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    requireParentFragment(),
                    cameraSelector,
                    videoCapture,
                    imageCapture,
                    preview
                )
            } catch (exc: Exception) {
                resetUIAndState()
            }

            binding.includeCameraFooter.imageViewCameraFooterShutter.setOnClickListener {
                lifecycleScope.launch(viewModel.io) {
                    val photoFile = viewModel.createFile(IMAGE_EXTENSION, true)

                    // Setup image capture metadata
                    val metadata = ImageCapture.Metadata().apply {
                        // Mirror image when using the front camera
                        isReversedHorizontal = cameraItem.lensFacing == LensFacing.Front
                    }

                    // Create output options object which contains file + metadata
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                        .setMetadata(metadata)
                        .build()

                    // Setup image capture listener which is triggered after photo has been taken
                    imageCapture.takePicture(
                        outputOptions,
                        cameraExecutor,
                        imageSavedCallback
                    )
                }

            }
            binding.includeCameraFooter.imageViewCameraFooterShutter.setOnLongClickListener { view ->
                lifecycleScope.launch(viewModel.mainImmediate) {
                    if (activeRecording == null || recordingState is VideoRecordEvent.Finalize) {
                        startRecording()
                    }
                }

                return@setOnLongClickListener true
            }

            binding.includeCameraFooter.imageViewCameraFooterShutter.setOnTouchListener { view, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    lifecycleScope.launch(viewModel.io) {
                        if (activeRecording != null && recordingState !is VideoRecordEvent.Finalize) {
                            activeRecording?.stop()
                            activeRecording = null
                            delay(200L)
                        }
                    }
                }
                return@setOnTouchListener view.onTouchEvent(motionEvent)
            }

            // re-enable button to switch between back/front camera
            binding.includeCameraFooter.imageViewCameraFooterBackFront.isEnabled = true
        }
    }

    private fun screenAspectRatio(): Int {
        // TODO: Get window width and height
//        width: Int, height: Int
//        val previewRatio = max(width, height).toDouble() / min(width, height)
//        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
//            return AspectRatio.RATIO_4_3
//        }
        return AspectRatio.RATIO_16_9
    }


    /**
     * UpdateUI according to CameraX VideoRecordEvent type
     */
    private fun updateUI(event: VideoRecordEvent) {
        lifecycleScope.launch(viewModel.mainImmediate) {
            binding.includeCameraFooter.imageViewCameraFooterShutter.setImageDrawable(
                AppCompatResources.getDrawable(requireContext(),
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            R.drawable.ic_shutter_recording
                        }
                        is VideoRecordEvent.Finalize-> {
                            R.drawable.ic_shutter
                        }
                        else -> {
                            R.drawable.ic_shutter_recording
                        }
                    }
                )
            )
        }
    }

    /**
     * ResetUI (restart):
     *    in case binding failed, let's give it another change for re-try. In future cases
     *    we might fail and user get notified on the status
     */
    private fun resetUIAndState() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            binding.includeCameraFooter.imageViewCameraFooterShutter.setImageDrawable(
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_shutter)
            )

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
            viewModel.createFile(VIDEO_EXTENSION, false)
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

    override suspend fun onViewStateFlowCollect(viewState: CameraViewState) {
        @Exhaustive
        when (viewState) {
            is CameraViewState.Idle -> {}
            is CameraViewState.Active -> {
                viewState.cameraItem?.let { item ->

                    // disable button to switch between back/front camera
                    binding.includeCameraFooter.imageViewCameraFooterBackFront.isEnabled = false

                    try {
                        rotationProvider?.removeListener(rotationListener)
                        startCamera(item)
                        rotationProvider?.addListener(mainThreadExecutor, rotationListener)
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
                binding.includeCameraMediaPreview.apply {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        @Exhaustive
                        when (viewState) {
                            is CapturePreviewViewState.Preview.VideoPreview -> {
                                if (viewState.media.exists()) {
                                    root.visible
                                    videoViewCameraVideoPreview.visible
                                    imageViewCameraImagePreview.gone
                                    imageViewVideoPreviewPlayPause.gone

                                    textViewCameraMediaPreviewUse.text = getString(R.string.camera_use_video)

                                    val uri = viewState.media.toUri()
                                    videoViewCameraVideoPreview.setVideoURI(uri)
                                    videoViewCameraVideoPreview.setOnPreparedListener { mediaPlayer ->
                                        mediaPlayer.start()
                                        imageViewVideoPreviewPlayPause.visible
                                        imageViewVideoPreviewPlayPause.setImageDrawable(
                                            AppCompatResources.getDrawable(
                                                requireContext(),
                                                R.drawable.ic_podcast_pause_circle
                                            )
                                        )
                                    }
                                    videoViewCameraVideoPreview.setOnCompletionListener {
                                        imageViewVideoPreviewPlayPause.setImageDrawable(
                                            AppCompatResources.getDrawable(
                                                requireContext(),
                                                R.drawable.ic_podcast_play_circle
                                            )
                                        )
                                    }
                                    imageViewVideoPreviewPlayPause.setOnClickListener {
                                        if (videoViewCameraVideoPreview.isPlaying) {
                                            videoViewCameraVideoPreview.pause()
                                            imageViewVideoPreviewPlayPause.setImageDrawable(
                                                AppCompatResources.getDrawable(
                                                    requireContext(),
                                                    R.drawable.ic_podcast_play_circle
                                                )
                                            )
                                        } else {
                                            videoViewCameraVideoPreview.start()
                                            imageViewVideoPreviewPlayPause.setImageDrawable(
                                                AppCompatResources.getDrawable(
                                                    requireContext(),
                                                    R.drawable.ic_podcast_pause_circle
                                                )
                                            )
                                        }
                                    }
                                    videoViewCameraVideoPreview.setOnErrorListener { _, _, _ ->
                                        return@setOnErrorListener true
                                    }
                                } else {
                                    viewModel.updateMediaPreviewViewState(CapturePreviewViewState.None)
                                }
                            }
                            is CapturePreviewViewState.Preview.ImagePreview -> {
                                if (viewState.media.exists()) {
                                    root.visible
                                    imageViewCameraImagePreview.visible
                                    videoViewCameraVideoPreview.gone
                                    imageViewVideoPreviewPlayPause.gone

                                    textViewCameraMediaPreviewUse.text = getString(R.string.camera_use_photo)
                                    imageLoader.load(imageViewCameraImagePreview, viewState.media)
                                } else {
                                    viewModel.updateMediaPreviewViewState(CapturePreviewViewState.None)
                                }
                            }
                            is CapturePreviewViewState.None -> {
                                root.gone

                                imageViewCameraImagePreview.setImageDrawable(null)

                                videoViewCameraVideoPreview.stopPlayback()
                                videoViewCameraVideoPreview.setVideoURI(null)
                            }
                        }
                    }

                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    companion object {
        private const val IMAGE_EXTENSION = "jpg"
        private const val VIDEO_EXTENSION = "mp4"
    }
}