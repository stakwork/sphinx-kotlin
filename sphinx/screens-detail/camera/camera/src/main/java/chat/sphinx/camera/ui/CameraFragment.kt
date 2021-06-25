package chat.sphinx.camera.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.camera.R
import chat.sphinx.camera.databinding.FragmentCameraBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import kotlinx.coroutines.launch

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
    }

    @Suppress("PrivatePropertyName")
    private val PERMISSIONS_REQUIRED = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    )

    override val binding: FragmentCameraBinding by viewBinding(FragmentCameraBinding::bind)
    override val viewModel: CameraViewModel by viewModels()

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

                startCamera()
            } catch (e: Exception) {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.submitSideEffect(
                        CameraSideEffect.Notify(getString(R.string.camera_permissions_required))
                    )
                }
            }
        }
    }

    private val cameraManager: CameraManager by lazy {
        requireActivity().applicationContext
            .getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private inner class ThreadHolder: DefaultLifecycleObserver {

        @Volatile
        private var cameraThread: HandlerThread? = null
        private val threadLock = Object()

        @Volatile
        private var cameraHandler: Handler? = null
        private val handlerLock = Object()

        fun getCameraThread(): HandlerThread =
            cameraThread ?: synchronized(threadLock) {
                cameraThread ?: HandlerThread("CameraThread").apply { start() }
                    .also {
                        cameraThread = it
                        viewLifecycleOwner.lifecycle.addObserver(this)
                    }
            }

        fun getCameraHandler(): Handler =
            cameraHandler ?: synchronized(handlerLock) {
                cameraHandler ?: Handler(getCameraThread().looper)
                    .also { cameraHandler = it }
            }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            synchronized(handlerLock) {
                synchronized(threadLock) {
                    val thread = cameraThread
                    cameraHandler = null
                    cameraThread = null
                    thread?.quitSafely()
                }
            }
        }
    }

    private val threadHolder = ThreadHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as InsetterActivity).addNavigationBarPadding(binding.root)

        if (hasPermissions(requireContext())) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(PERMISSIONS_REQUIRED)
        }
    }

    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        // TODO: Implement
    }

    override suspend fun onSideEffectCollect(sideEffect: CameraSideEffect) {
        sideEffect.execute(requireActivity())
    }

    override suspend fun onViewStateFlowCollect(viewState: CameraViewState) {
//        TODO("Not yet implemented")
    }
}
