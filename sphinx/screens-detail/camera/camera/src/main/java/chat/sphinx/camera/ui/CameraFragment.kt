package chat.sphinx.camera.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActionBar
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ExifInterface
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.marginLeft
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
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
import com.example.android.camera.utils.computeExifOrientation
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
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
internal class CameraFragment: SideEffectFragment<
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
    private val imageReaderThreadHolder = ThreadHolder()

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

                // calculate the primary window's screen height
                val primaryWindowHeight =
                    (detailFragmentHeight / (1F - spaceDetailPct.float)) +
                    (requireActivity() as InsetterActivity).statusBarInsetHeight.top

                val spaceTop = primaryWindowHeight * spaceDetailPct.float

                val viewWidth = (spaceTop / 2) + 1 + Dp(4F).toPx(binding.root.context).value

                binding.viewCameraSpaceEnd.apply {
                    layoutParams.width = viewWidth.toInt()
                }

                binding.viewCameraSpaceStart.apply {
                    layoutParams.width = viewWidth.toInt()
                }

                binding.includeCameraImagePreview.apply {
                    spaceCameraImagePreviewEnd.apply space@ {
                        this@space.layoutParams.width = viewWidth.toInt()
                    }
                    spaceCameraImagePreviewStart.apply space@ {
                        this@space.layoutParams.width = viewWidth.toInt()
                    }
                }

                Px(viewWidth)
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
            textViewCameraImagePreviewRetake.setOnClickListener {
                @Exhaustive
                when (val vs = viewModel.currentCapturePreviewViewState) {
                    is CapturePreviewViewState.None -> {}
                    is CapturePreviewViewState.Preview -> {
                        viewModel.deleteImage(vs.value)
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

        binding.includeCameraFooter.textViewCameraFooterCancel.setOnClickListener {
            viewModel.processCancellationResponse()
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
                    takePhoto(
                        cameraItem,
                        imageReader,
                        session,
                    ).use { result ->

                        val output = saveResult(cameraItem, result)

                        // If the result is a JPEG file, update EXIF metadata with orientation info
                        if (output.extension == "jpg") {
                            val exif = ExifInterface(output.absolutePath)
                            exif.setAttribute(
                                ExifInterface.TAG_ORIENTATION,
                                result.orientation.toString()
                            )
                            exif.saveAttributes()
                        }

                        viewModel.updateImagePreviewViewState(
                            CapturePreviewViewState.Preview.ImagePreview(output)
                        )
                    }

                    delay(200L)
                    view.post { view.isEnabled = true }
                }
            }

            // re-enable button to switch between back/front camera
            binding.includeCameraFooter.imageViewCameraFooterBackFront.isEnabled = true
        }
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

    private val animationTask: Runnable by lazy {
        Runnable {
            binding.viewCameraOverlay.apply{
                background = Color.argb(150, 255, 255, 255).toDrawable()
                postDelayed(
                    {
                        background = null
                    },
                    ANIMATION_FAST_MILLIS
                )
            }
        }
    }

    private suspend fun takePhoto(
        cameraListItem: CameraItem,
        imageReader: ImageReader,
        session: CameraCaptureSession
    ): CombinedCaptureResult =
        suspendCoroutine { cont ->

            // Flush any images left in the image reader
            @Suppress("ControlFlowWithEmptyBody")
            while (imageReader.acquireNextImage() != null) {}

            val imageQueue = ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)
            imageReader.setOnImageAvailableListener(
                { reader ->
                    val image = reader.acquireNextImage()
                    imageQueue.add(image)
                },
                cameraThreadHolder.getHandler()
            )

            val captureRequest = session
                .device
                .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(imageReader.surface)
                }

            session.capture(
                captureRequest.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureStarted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        timestamp: Long,
                        frameNumber: Long
                    ) {
                        super.onCaptureStarted(session, request, timestamp, frameNumber)
                        binding.autoFitSurfaceViewCamera.post(animationTask)
                    }

                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)

                        // Set a timeout in case image captured is dropped from the pipeline
                        val exc = TimeoutException("Image dequeuing took too long")
                        val timeoutRunnable = Runnable { cont.resumeWithException(exc) }

                        imageReaderThreadHolder.getHandler().postDelayed(timeoutRunnable, IMAGE_CAPTURE_TIMEOUT_MILLIS)

                        // Loop in the coroutine's context until an image with matching timestamp comes
                        // We need to launch the coroutine context again because the callback is done in
                        //  the handler provided to the `capture` method, not in our coroutine context
                        @Suppress("BlockingMethodInNonBlockingContext")
                        lifecycleScope.launch(cont.context) {
                            while (true) {
                                // Dequeue images while timestamps don't match
                                val image = imageQueue.take()

                                if (
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                    image.format != ImageFormat.DEPTH_JPEG &&
                                    image.timestamp != resultTimestamp
                                ) {
                                    continue
                                }

                                // Unset the image reader listener
                                imageReaderThreadHolder.getHandler().removeCallbacks(timeoutRunnable)
                                imageReader.setOnImageAvailableListener(null, null)

                                // Clear the queue of images, if there are left
                                while (imageQueue.size > 0) {
                                    imageQueue.take().close()
                                }

                                // Compute EXIF orientation metadata
                                val rotation = orientationLiveData?.value ?: Surface.ROTATION_0
                                val mirrored = cameraListItem
                                    .characteristics
                                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                                val exifOrientation = computeExifOrientation(rotation, mirrored)

                                // Build the result and resume progress
                                cont.resume(
                                    CombinedCaptureResult(
                                        image,
                                        result,
                                        exifOrientation,
                                        imageReader.imageFormat
                                    )
                                )
                            }
                        }
                    }
                },
                cameraThreadHolder.getHandler()
            )
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun saveResult(
        cameraItem: CameraItem,
        result: CombinedCaptureResult,
    ): File = suspendCoroutine { cont ->
        when (result.format) {

            ImageFormat.JPEG,
            ImageFormat.DEPTH_JPEG -> {
                val buffer = result.image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }

                try {
                    val output = viewModel.createFile("jpg", image = true)
                    FileOutputStream(output).use { it.write(bytes) }
                    cont.resume(output)
                } catch (e: IOException) {
                    cont.resumeWithException(e)
                }
            }

            ImageFormat.RAW_SENSOR -> {
                val dngCreator = DngCreator(cameraItem.characteristics, result.metadata)

                try {
                    val output = viewModel.createFile("dng", image = true)
                    FileOutputStream(output).use { dngCreator.writeImage(it, result.image) }
                } catch (e: IOException) {
                    cont.resumeWithException(e)
                }
            }

            else -> {
                cont.resumeWithException(RuntimeException("Unknown image format: ${result.image.format}"))
            }
        }
    }

    data class CombinedCaptureResult(
        val image: Image,
        val metadata: CaptureResult,
        val orientation: Int,
        val format: Int
    ) : Closeable {
        override fun close() = image.close()
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
                        is CapturePreviewViewState.Preview.ImagePreview -> {
                            if (viewState.value.exists()) {
                                root.visible
                                imageLoader.load(imageViewCameraImagePreview, viewState.value)
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
