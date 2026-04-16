package com.sonyairplay.receiver

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

object FFmpegAlacDecoder {
    private const val TAG = "FFmpegAlacDecoder"

    fun decodeAlacToPcm(inputFilePath: String, outputPcmPath: String, onComplete: (Boolean, String) -> Unit) {
        val cmd = "-y -i \"$inputFilePath\" -f s16le -acodec pcm_s16le -ac 2 -ar 44100 \"$outputPcmPath\""
        FFmpegKit.executeAsync(cmd) { session ->
            val rc = session.returnCode
            if (ReturnCode.isSuccess(rc)) {
                Log.i(TAG, "Decoded $inputFilePath -> $outputPcmPath")
                onComplete(true, outputPcmPath)
            } else {
                Log.e(TAG, "FFmpeg failed rc=${'$'}rc")
                onComplete(false, outputPcmPath)
            }
        }
    }
}
