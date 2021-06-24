package chat.sphinx.camera.ui

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.camera.R
import chat.sphinx.camera.databinding.FragmentCameraBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment

@AndroidEntryPoint
internal class CameraFragment: SideEffectFragment<
        FragmentActivity,
        CameraSideEffect,
        CameraViewState,
        CameraViewModel,
        FragmentCameraBinding,
        >(R.layout.fragment_camera)
{
    override val binding: FragmentCameraBinding by viewBinding(FragmentCameraBinding::bind)
    override val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel
    }

    override suspend fun onSideEffectCollect(sideEffect: CameraSideEffect) {
//        TODO("Not yet implemented")
    }

    override suspend fun onViewStateFlowCollect(viewState: CameraViewState) {
//        TODO("Not yet implemented")
    }
}
