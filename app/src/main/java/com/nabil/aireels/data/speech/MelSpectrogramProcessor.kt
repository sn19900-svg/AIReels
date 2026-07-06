package com.nabil.aireels.data.speech

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

class MelSpectrogramProcessor(
    private val sampleRate: Int = 16000,
    private val winLength: Int = 400,
    private val nFft: Int = 512,
    private val hopLength: Int = 160,
    private val nMels: Int = 80
) {

    fun process(audioSamples: FloatArray): Array<FloatArray> {
        val window = hannWindow(winLength)
        val melFilterBank = createMelFilterBank()
        val frames = mutableListOf<FloatArray>()

        var offset = 0
        while (offset + winLength <= audioSamples.size) {
            val frame = FloatArray(nFft)
            for (i in 0 until winLength) {
                frame[i] = audioSamples[offset + i] * window[i]
            }

            val (real, imag) = fft(frame)
            val numBins = nFft / 2 + 1
            val powerSpectrum = FloatArray(numBins)
            for (i in 0 until numBins) {
                powerSpectrum[i] = real[i] * real[i] + imag[i] * imag[i]
            }

            val melEnergies = FloatArray(nMels)
            for (m in 0 until nMels) {
                var sum = 0.0f
                for (k in 0 until numBins) {
                    sum += powerSpectrum[k] * melFilterBank[m][k]
                }
                melEnergies[m] = ln(max(sum, 1e-10f))
            }
            frames.add(melEnergies)
            offset += hopLength
        }
        return frames.toTypedArray()
    }

    private fun hannWindow(size: Int): FloatArray {
        return FloatArray(size) { i ->
            (0.5 * (1.0 - cos(2.0 * PI * i / (size - 1)))).toFloat()
        }
    }

    private fun fft(input: FloatArray): Pair<FloatArray, FloatArray> {
        val real = input.copyOf()
        val imag = FloatArray(input.size)
        fftInPlace(real, imag)
        return Pair(real, imag)
    }

    private fun fftInPlace(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n <= 1) return

        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tempReal = real[i]; real[i] = real[j]; real[j] = tempReal
                val tempImag = imag[i]; imag[i] = imag[j]; imag[j] = tempImag
            }
        }

        var length = 2
        while (length <= n) {
            val angle = -2.0 * PI / length
            val wReal = cos(angle).toFloat()
            val wImag = sin(angle).toFloat()
            var i = 0
            while (i < n) {
                var curWReal = 1.0f
                var curWImag = 0.0f
                for (k in 0 until length / 2) {
                    val evenIndex = i + k
                    val oddIndex = i + k + length / 2
                    val evenReal = real[evenIndex]
                    val evenImag = imag[evenIndex]
                    val oddReal = real[oddIndex] * curWReal - imag[oddIndex] * curWImag
                    val oddImag = real[oddIndex] * curWImag + imag[oddIndex] * curWReal
                    real[evenIndex] = evenReal + oddReal
                    imag[evenIndex] = evenImag + oddImag
                    real[oddIndex] = evenReal - oddReal
                    imag[oddIndex] = evenImag - oddImag
                    val nextWReal = curWReal * wReal - curWImag * wImag
                    val nextWImag = curWReal * wImag + curWImag * wReal
                    curWReal = nextWReal
                    curWImag = nextWImag
                }
                i += length
            }
            length = length shl 1
        }
    }

    private fun createMelFilterBank(): Array<FloatArray> {
        val numFftBins = nFft / 2 + 1
        val filterBank = Array(nMels) { FloatArray(numFftBins) }

        val melMin = hzToMel(0.0)
        val melMax = hzToMel(sampleRate / 2.0)
        val melPoints = DoubleArray(nMels + 2) { i ->
            melMin + i * (melMax - melMin) / (nMels + 1)
        }
        val hzPoints = melPoints.map { melToHz(it) }
        val binPoints = hzPoints.map { floor((nFft + 1) * it / sampleRate).toInt() }

        for (m in 1..nMels) {
            val left = binPoints[m - 1]
            val center = binPoints[m]
            val right = binPoints[m + 1]

            if (center != left) {
                for (k in left until center) {
                    if (k in 0 until numFftBins) {
                        filterBank[m - 1][k] = (k - left).toFloat() / (center - left).toFloat()
                    }
                }
            }
            if (right != center) {
                for (k in center until right) {
                    if (k in 0 until numFftBins) {
                        filterBank[m - 1][k] = (right - k).toFloat() / (right - center).toFloat()
                    }
                }
            }
        }
        return filterBank
    }

    private fun hzToMel(hz: Double): Double = 2595.0 * log10(1.0 + hz / 700.0)
    private fun melToHz(mel: Double): Double = 700.0 * (10.0.pow(mel / 2595.0) - 1.0)
}
