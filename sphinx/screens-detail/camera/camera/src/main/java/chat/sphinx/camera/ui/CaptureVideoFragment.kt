package chat.sphinx.camera.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.camera.R
import chat.sphinx.camera.databinding.FragmentCameraBinding
import chat.sphinx.camera.model.CameraItem
import chat.sphinx.camera.ui.viewstate.CameraViewState
import chat.sphinx.camera.ui.viewstate.CapturePreviewViewState
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.resources.toPx
import chat.sphinx.wrapper_view.Dp
import chat.sphinx.wrapper_view.Px
import com.example.android.camera.utils.OrientationLiveData
import com.example.android.camera.utils.getPreviewOutputSize
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
internal class CaptureVideoFragment: SideEffectFragment<
        FragmentActivity,
        CameraSideEffect,
        CameraViewState,
        CameraViewModel,
        FragmentCameraBinding,
        >(R.layout.fragment_camera)
{
    companion object {
        const val ANIMATION_FAST_MILLIS = 50L
        const val ANIMATION_SLOW_MILLIS = 100L

        const val IMAGE_BUFFER_SIZE = 3
        const val IMAGE_CAPTURE_TIMEOUT_MILLIS: Long = 5_000
    }

    @Suppress("PrivatePropertyName")
    private val PERMISSIONS_REQUIRED = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    )

    override val binding: FragmentCameraBinding by viewBinding(FragmentCameraBinding::bind)
    override val viewModel: CameraViewModel by viewModels()

    private lateinit var videoCapture: VideoCapture<Recorder>
    private var activeRecording: ActiveRecording? = null
    private lateinit var recordingState: VideoRecordEvent

    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(requireContext()) }

    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status)
            recordingState = event

//        updateUI(event)

        if (event is VideoRecordEvent.Finalize) {
            event.outputResults.outputUri.path?.let {
                viewModel.updateImagePreviewViewState(
                    CapturePreviewViewState.Preview.VideoPreview(File(it))
                )
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

    private inner class ThreadHolder: DefaultLifecycleObserver {

        @Volatile
        private var thread: HandlerThread? = null
        private val threadLock = Object()

        @Volatile
        private var handler: Handler? = null
        private val handlerLock = Object()

        fun getThread(): HandlerThread =
            thread ?: synchronized(threadLock) {
                thread ?: HandlerThread("CameraThread").apply { start() }
                    .also {
                        thread = it
                        lifecycleScope.launch(viewModel.main) {
                            viewLifecycleOwner.lifecycle.addObserver(this@ThreadHolder)
                        }
                    }
            }

        fun getHandler(): Handler =
            handler ?: synchronized(handlerLock) {
                handler ?: Handler(getThread().looper)
                    .also { handler = it }
            }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            synchronized(handlerLock) {
                synchronized(threadLock) {
                    val thread = thread
                    handler = null
                    this.thread = null
                    thread?.quitSafely()
                }
            }
        }
    }

    private val cameraThreadHolder = ThreadHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel
    }

    @Volatile
    private var orientationLiveData: OrientationLiveData? = null
    private val surfaceHolderState: MutableStateFlow<SurfaceHolder?> by lazy {
        MutableStateFlow(null)
    }

    private inner class SpaceWidthSetter: DefaultLifecycleObserver {
        private var width: Px? = null
        private val lock = Mutex()

        // if width is null, it will set them after the view is setup
        // otherwise it does nothing (b/c they're already set
        suspend fun setCameraSpacessIfNeeded() {
            width ?: lock.withLock {
                width ?: setSpaces()
                    .also { width = it }
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            width = null
        }

        private suspend fun setSpaces(): Px {
            try {
                // wait for the screen to be created
                surfaceHolderState.value ?: surfaceHolderState.collect { holder ->
                    if (holder != null) {
                        throw Exception()
                    }
                }
            } catch (e: Exception) {}

            return withContext(viewModel.main) {

                viewLifecycleOwner.lifecycle.addObserver(this@SpaceWidthSetter)

                val spaceDetailPct = TypedValue()

                // get space resource percentage height for detail screen
                binding.root.context.resources.getValue(
                    R.dimen.space_detail_host_height,
                    spaceDetailPct,
                    true
                )

                val detailFragmentHeight = binding.root.measuredHeight.toFloat()
                val detailFragmentWidth = binding.root.measuredWidth.toFloat()

                // calculate the primary window's screen height
                val primaryWindowHeight =
                    (detailFragmentHeight / (1F - spaceDetailPct.float)) +
                            (requireActivity() as InsetterActivity).statusBarInsetHeight.top

                val spaceTop = primaryWindowHeight * spaceDetailPct.float

                val viewWidth = (spaceTop / 2) + 1 + Dp(4F).toPx(binding.root.context).value

                binding.autoFitSurfaceViewCamera.apply {
                    layoutParams.width = (detailFragmentWidth + (viewWidth * 2)).toInt()
                }

                Px(primaryWindowHeight)
            }
        }
    }

    private val spaceWidthSetter = SpaceWidthSetter()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.includeCameraFooter.root)
            .addNavigationBarPadding(binding.includeCameraImagePreview.layoutConstraintCameraImagePreviewFooter)

        binding.autoFitSurfaceViewCamera.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                surfaceHolderState.value = null
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {}

            override fun surfaceCreated(holder: SurfaceHolder) {
                surfaceHolderState.value = holder
            }
        })

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
                        viewModel.updateImagePreviewViewState(CapturePreviewViewState.None)
                    }
                    is CapturePreviewViewState.Preview.VideoPreview -> {
                        viewModel.deleteImage(vs.media)
                        viewModel.updateImagePreviewViewState(CapturePreviewViewState.None)
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

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun startCamera(cameraItem: CameraItem) {
        lifecycleScope.launch(viewModel.main) {
            val camera = openCamera(
                viewModel.cameraManager,
                cameraItem.cameraId,
                cameraThreadHolder.getHandler(),
            ).also {
                camera = it
            }

            // TOOD: add Image Format to CameraListItem
            val size = cameraItem.configMap.getOutputSizes(ImageFormat.JPEG)
                .maxByOrNull { it.height * it.width }!!

            val imageReader = ImageReader.newInstance(
                size.width, size.height, ImageFormat.JPEG, IMAGE_BUFFER_SIZE
            )

            val targets = listOf(binding.autoFitSurfaceViewCamera.holder.surface, imageReader.surface)

            val session = createCaptureSession(
                camera,
                targets,
                cameraThreadHolder.getHandler()
            )

            val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(binding.autoFitSurfaceViewCamera.holder.surface)
            }

            session.setRepeatingRequest(captureRequest.build(), null, cameraThreadHolder.getHandler())

            binding.includeCameraFooter.imageViewCameraFooterShutter.setOnClickListener { view ->

                view.isEnabled = false

                lifecycleScope.launch(viewModel.io) {
                    if (activeRecording == null || recordingState is VideoRecordEvent.Finalize) {
//                    fragmentCameraBinding.captureButton.setImageResource(androidx.camera.video.R.drawable.ic_pause)
//                    fragmentCameraBinding.stopButton.visibility = View.VISIBLE
//                    enableUI(false)
                        startRecording()
                    } else {
                        when (recordingState) {
                            is VideoRecordEvent.Start -> {
                                activeRecording?.pause()
//                            fragmentCameraBinding.stopButton.visibility = View.VISIBLE
                            }
                            is VideoRecordEvent.Pause -> {
                                activeRecording?.resume()
                            }
                            is VideoRecordEvent.Resume -> {
                                activeRecording?.pause()
                            }
                            else -> {

                            }
                        }
                    }

                    delay(200L)
                    view.post { view.isEnabled = true }
                }
            }

            // re-enable button to switch between back/front camera
            binding.includeCameraFooter.imageViewCameraFooterBackFront.isEnabled = true
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
        activeRecording =
            videoCapture.output.prepareRecording(requireActivity(), fileOutputOptions)
                .withEventListener(
                    mainThreadExecutor,
                    captureListener
                )
                .withAudioEnabled()
                .start()
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
        handler: Handler? = null
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        try {
            camera?.close()
            camera = null
        } catch (e: Exception) {}
        manager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) = cont.resume(device)

                override fun onDisconnected(device: CameraDevice) {}

                override fun onError(device: CameraDevice, error: Int) {
                    val msg = when(error) {
                        ERROR_CAMERA_DEVICE -> "Fatal (device)"
                        ERROR_CAMERA_DISABLED -> "Device policy"
                        ERROR_CAMERA_IN_USE -> "Camera in use"
                        ERROR_CAMERA_SERVICE -> "Fatal (service)"
                        ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                        else -> "Unknown"
                    }
                    val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                    if (cont.isActive) cont.resumeWithException(exc)
                }
            },
            handler,
        )
    }

    /**
     * Starts a [CameraCaptureSession] and returns the configured session (as the result of the
     * suspend coroutine
     */
    private suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->

        // Create a capture session using the predefined targets; this also involves defining the
        // session state callback to be notified of when the session is ready
        device.createCaptureSession(targets, object: CameraCaptureSession.StateCallback() {

            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                cont.resumeWithException(exc)
            }
        }, handler)
    }

    override suspend fun onSideEffectCollect(sideEffect: CameraSideEffect) {
        sideEffect.execute(requireActivity())
    }

    private val orientationObserver = Observer<Int> { orientation ->

    }

    override suspend fun onViewStateFlowCollect(viewState: CameraViewState) {

        binding.includeCameraFooter.imageViewCameraFooterShutter.setOnClickListener(null)

        @Exhaustive
        when (viewState) {
            is CameraViewState.Idle -> {}
            is CameraViewState.Active -> {
                viewState.cameraItem?.let { item ->

                    // disable button to switch between back/front camera
                    binding.includeCameraFooter.imageViewCameraFooterBackFront.isEnabled = false

                    try {
                        surfaceHolderState.collect { holder ->
                            if (holder != null) {
                                spaceWidthSetter.setCameraSpacessIfNeeded()

                                val previewSize = getPreviewOutputSize(
                                    binding.autoFitSurfaceViewCamera.display,
                                    item.characteristics,
                                    SurfaceHolder::class.java,
                                )

                                binding.autoFitSurfaceViewCamera.setAspectRatio(
                                    previewSize.width,
                                    previewSize.height
                                )

                                orientationLiveData?.removeObserver(orientationObserver)
                                orientationLiveData = OrientationLiveData(binding.root.context, item.characteristics).apply {
                                    observe(viewLifecycleOwner, orientationObserver)
                                }

                                startCamera(item)

                                throw Exception()
                            }
                        }
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
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
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
                                viewModel.updateImagePreviewViewState(CapturePreviewViewState.None)
                            }
                        }
                        is CapturePreviewViewState.Preview.ImagePreview -> {
                            if (viewState.media.exists()) {
                                root.visible
                                imageLoader.load(imageViewCameraImagePreview, viewState.media)
                            } else {
                                viewModel.updateImagePreviewViewState(CapturePreviewViewState.None)
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
