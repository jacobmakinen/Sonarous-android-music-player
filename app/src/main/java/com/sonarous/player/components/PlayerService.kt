package com.sonarous.player.components

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sonarous.player.VisualiserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jtransforms.fft.DoubleFFT_1D
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sqrt

@UnstableApi
class PlayerService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    object AudioVisualizerProcessor : AudioProcessor {
        private val _visualizerStateFlow = MutableStateFlow(
            VisualiserData(doubleArrayOf(), 0.0)
        )
        val visualizerStateFlow: StateFlow<VisualiserData> = _visualizerStateFlow.asStateFlow()
        var speed = 1f
        var pitch = 1f
        var visualiserIsOn = false
        private val sonicAudioProcessor = SonicAudioProcessor()
        private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
        private const val ARRAY_SIZE = 512
        private var fft = DoubleFFT_1D(ARRAY_SIZE.toLong())
        private var endOfStreamQueued = false
        private var isEnded = false
        var visualizerList = DoubleArray(7)
        var volume = 0.0
        var usingSonicProcessor = false
        private val emissionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            if (usingSonicProcessor) {
                sonicAudioProcessor.configure(inputAudioFormat)
                // Factors must not be 1 or 0 -> crash
                if (speed != 1f && speed != 0f) {
                    sonicAudioProcessor.setSpeed(speed)
                }
                if (pitch != 1f && pitch != 0f) {
                    sonicAudioProcessor.setPitch(pitch)
                }
            }
            return inputAudioFormat
        }

        override fun isActive(): Boolean = true

        override fun queueInput(inputBuffer: ByteBuffer) {
            if (!inputBuffer.hasRemaining()) {
                return
            }
            if (usingSonicProcessor) {
                sonicAudioProcessor.queueInput(inputBuffer)
            } else {
                outputBuffer = inputBuffer
            }
        }

        override fun queueEndOfStream() {
            endOfStreamQueued = true
            sonicAudioProcessor.queueEndOfStream()
        }

        override fun getOutput(): ByteBuffer {
            val soundBuffer = if (usingSonicProcessor) {
                sonicAudioProcessor.output
            } else {
                outputBuffer
            }
            if (visualiserIsOn) {
                sendVisualizerData(soundBuffer)
            }
            //================================= End of equaliser processing =================================//
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            if (endOfStreamQueued) {
                isEnded = true
            }
            return soundBuffer
        }

        private fun sendVisualizerData(soundBuffer: ByteBuffer) {
            //============================ Collecting buffer data ============================//
            val fftArray = DoubleArray(ARRAY_SIZE)
            var bufferVolume = getFftData(soundBuffer, fftArray)

            //================================= Visualizer data =================================//

            val absValueList = DoubleArray(fftArray.count() / 2)
            var i = 0
            while (i < fftArray.count() / 2) {
                val real = fftArray[i * 2]
                val imaginary = fftArray[i * 2 + 1]
                absValueList[i] = sqrt(real * real + imaginary * imaginary)
                i++
            }
            bufferVolume = sqrt(bufferVolume / ARRAY_SIZE)
            visualizerList = frequencyCalculator(absValueList)
            volume = bufferVolume

            val capturedVisualizerList = visualizerList.copyOf()
            val capturedVolume = volume

            emissionScope.launch {
                delay(500)
                _visualizerStateFlow.emit(
                    VisualiserData(capturedVisualizerList, capturedVolume)
                )
            }
        }

        /**
         * Calculates the FFT of the input buffer and fills the fftArray with the result.
         * The volume of the buffer is returned
         */
        private fun getFftData(inputBuffer: ByteBuffer, fftArray: DoubleArray): Double {
            val shortBuffer = inputBuffer.asShortBuffer()
            var buffer: Short
            var bufferVolume = 0.0
            for (i in 0 until ARRAY_SIZE) {
                try {
                    buffer = shortBuffer.get()
                    bufferVolume += (buffer * buffer).toDouble() // To cancel out the - & + values
                    fftArray[i] = buffer / 32768.0 // Normalisation
                } catch (_: BufferUnderflowException) {
                    fftArray[i] = 0.0
                    bufferVolume += 0.0
                }
                if (fftArray[i].isNaN() or fftArray[i].isInfinite()) { // Prevent float NaN's
                    fftArray[i] = 0.0
                    bufferVolume += 0.0
                }
                val window = 0.5 * (1 - cos(2.0 * Math.PI * i / (ARRAY_SIZE - 1))) // Hann window to reduce sound leakage
                fftArray[i] = fftArray[i] * window
            }
            //================================= Visualizer data =================================//
            fft.realForward(fftArray)
            return bufferVolume
        }

        override fun isEnded(): Boolean = isEnded

        override fun flush() {
            emissionScope.coroutineContext.cancelChildren()
            if (usingSonicProcessor) {
                sonicAudioProcessor.flush()
            }
            isEnded = false
            endOfStreamQueued = false
            outputBuffer = AudioProcessor.EMPTY_BUFFER
        }

        override fun reset() {
            emissionScope.coroutineContext.cancelChildren() // Cancel pending delayed emissions

            if (usingSonicProcessor) {
                sonicAudioProcessor.reset()
            }
            isEnded = false
            endOfStreamQueued = false
            outputBuffer = AudioProcessor.EMPTY_BUFFER
        }
        private fun frequencyCalculator(absValueList: DoubleArray): DoubleArray {
            val tempList = DoubleArray(7)
            tempList[0] = (absValueList[1])
            tempList[1] = (absValueList[2])
            tempList[2] = (absValueList[5])
            tempList[3] = (absValueList[13])
            tempList[4] = (absValueList[32])
            tempList[5] = (absValueList[80])
            tempList[6] = (absValueList[205])
            return tempList
        }
    }
}