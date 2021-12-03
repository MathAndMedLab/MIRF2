package com.mirf.features.ecg.util

import com.mirf.core.data.medimage.BufferedImageRawImage
import com.mirf.core.data.medimage.BufferedImageSeries
import com.mirf.features.ecg.data.EcgAttributes
import com.mirf.features.ecg.data.EcgData
import com.mirf.features.ecg.data.EcgLeadType
import java.awt.Color
import java.util.*
import java.util.stream.IntStream


object EcgBeatExtractor {
    /**
     * Return an array with R-peaks indices.
     * This implementation of Pan Tompkins algorithm is only suitable for 360 frequency signals.
     */
    fun extractBeatImages(ecgData: EcgData, leadType: EcgLeadType): BufferedImageSeries {
        val peaks = detectRPeaks(ecgData, leadType)
        val lead = ecgData.getAnalogSignal(leadType)
        val beatImages = ArrayList<BufferedImageRawImage>(peaks.size - 2)

        for (i in 1..peaks.size - 2) {
            val beatInterval = lead.copyOfRange(peaks[i - 1], peaks[i + 1] - 40)

            val x = DoubleArray(beatInterval.size)
            IntStream.range(0, beatInterval.size)
                .forEach { x[it] = it.toDouble() + 1 }

            val plot = SignalPlot()

            plot.plot(x, beatInterval, "-", Color(100, 100, 255), 1.0f)
            plot.displayGrid(false, false)
            plot.displayAxis(false, false)

            beatImages.add(BufferedImageRawImage(plot.getBufferedImage(128, 128)))

            plot.saveAsJpeg("beat231_$i.jpg", 128, 128)
        }
        return BufferedImageSeries(beatImages)
    }

    fun detectRPeaks(ecgData: EcgData, leadType: EcgLeadType): List<Int> {

        ecgData.attributes.getAttributeValue(EcgAttributes.LEADS)[leadType]

        val lead = ecgData.getAnalogSignal(leadType)

        val numeratorCoeffsLowPass = doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0)
        val denominatorCoeffsLowPass = doubleArrayOf(1.0, -2.0, 1.0)
        val numeratorCoeffsHighPass =
            doubleArrayOf(-1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                32.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0)
        val denominatorCoeffsHighPass = doubleArrayOf(1.0, 1.0)

        val filter = { numeratorCoeffs: DoubleArray, denominatorCoeffs: DoubleArray, llead: DoubleArray ->
            val filtered = DoubleArray(llead.size)
            for (i in llead.indices) {
                var curr = 0.0
                for (j in numeratorCoeffs.indices) {
                    if (i - j >= 0)
                        curr += numeratorCoeffs[j] * llead[i - j]
                }
                for (j in 1 until denominatorCoeffs.size) {
                    if (i - j >= 0)
                        curr -= denominatorCoeffs[j] * filtered[i - j]
                }
                filtered[i] = curr / denominatorCoeffs[0]
            }
            filtered
        }

        val filteredLowPass = filter(numeratorCoeffsLowPass, denominatorCoeffsLowPass, lead)

        val filteredHighPass = filter(numeratorCoeffsHighPass, denominatorCoeffsHighPass, filteredLowPass)

        val t = 360.0

        val derivative = DoubleArray(lead.size)

        for (i in 2..lead.size - 3)
            derivative[i] = (-1 * filteredHighPass[i - 2] - 2 * filteredHighPass[i - 1]
                    + 2 * filteredHighPass[i + 1] + filteredHighPass[i + 2]) / (8 * t)


        val squared = derivative.map { it * it }.toDoubleArray()

        val windowAverage = DoubleArray(lead.size)

        val windowSize = 30
        for (i in windowSize until squared.size - windowSize) {
            for (j in 0 until windowSize) {
                windowAverage[i] += squared[i - j]
            }
            windowAverage[i] /= windowSize.toDouble()
        }

        var peaki = windowAverage[0] //overall peak
        var spki = 0.0 // running estimate of the signal peak
        var npki = 0.0 // running estimate of the noise peak

        val noise = DoubleArray(lead.size)
        for (i in lead.indices)
            noise[i] = (lead[i] - filteredHighPass[i]) / (36.0 * 32.0)


        val peakIndices = LinkedList<Int>()
        //val peakValues = LinkedList<Double>()
        peakIndices.add(0)

        var threshold1i: Double
        var threshold2i: Double

        for (i in 1 until windowAverage.size) {
            if (windowAverage[i] > peaki)
                peaki = windowAverage[i]

            npki = (npki * (i - 1) + noise[i]) / i.toDouble()
            spki = (spki * (i - 1) + windowAverage[i]) / i.toDouble()
            spki = 0.875 * spki + 0.125 * peaki
            npki = 0.875 * npki + 0.125 * peaki

            threshold1i = npki + 0.25 * (spki - npki)
            threshold2i = 0.5 * threshold1i

            if (windowAverage[i] > threshold2i) {
                if (peakIndices.last + windowSize < i) {
                    peakIndices.add(i)
//                        peakValues.add(windowAverage[i])
                }
            }
        }

        val peakIndicesf = LinkedList<Int>()
        peakIndicesf.addAll(peakIndices)
        //val peakValuesf = LinkedList<Double>()

        var threshold1f: Double
        var threshold2f: Double
        var peakf = windowAverage[0]
        var spkf: Double
        var npkf: Double

        var average = 0.0
        for (i in 1 until peakIndices.size)
            average += peakIndices[i] - peakIndices[i - 1]
        average /= (peakIndices.size - 1).toDouble()

        for (i in 1 until peakIndices.size) {
            peakf = windowAverage[i - 1]
            spkf = 0.0
            npkf = 0.0
            if (peakIndices[i] - peakIndices[i - 1] > 1.66 * average) {
                for (j in peakIndices[i - 1] until peakIndices[i]) {
                    if (windowAverage[j] > peakf)
                        peakf = windowAverage[j]
                    npkf = (npkf * (j - 1) + noise[j]) / j.toDouble()
                    spkf = (spkf * (j - 1) + windowAverage[j]) / j.toDouble()
                    spkf = 0.875 * spkf + 0.125 * peakf
                    npkf = 0.875 * npkf + 0.125 * peakf

                    threshold1f = (npkf + 0.25 * (spkf - npkf))
                    threshold2f = 0.5 * threshold1f

                    if (windowAverage[j] > threshold2f) {
                        if (peakIndicesf.last < j) {
                            peakIndices.add(j)
                            //peakValues.add(windowAverage[j])
                        }
                    }
                }
            }
        }

        peakIndices.removeFirst()
        return peakIndices
    }
}