package chat.sphinx.chat_common.util

import android.media.MediaRecorder
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import java.io.File

class SphinxMediaRecorder(val mediaCacheHandler: MediaCacheHandler): MediaRecorder() {
    var recordingTempFile: File? = null

    fun startAudioRecording() {
        recordingTempFile = mediaCacheHandler.createAudioFile(".m4a")

        setAudioSource(AudioSource.MIC)
        setOutputFormat(OutputFormat.MPEG_4)
        setOutputFile(recordingTempFile?.absolutePath)
        setAudioEncoder(AudioEncoder.HE_AAC)

        prepare()

        start()
    }

    fun stopAudioRecording() {
        stop()
        release()
    }

    fun isRecording(): Boolean {
        return recordingTempFile != null
    }
}