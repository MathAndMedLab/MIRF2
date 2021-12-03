package com.mirf.features.ecg.reportpdf

import com.mirf.features.ecg.data.EcgArrhythmiaType
import com.mirf.features.ecg.data.EcgAttributes
import com.mirf.features.ecg.data.EcgData
import com.mirf.features.ecg.data.EcgLeadType
import com.mirf.features.ecg.util.EcgDiagnosis
import com.mirf.features.ecg.util.PatientInfo
import com.mirf.features.ecg.util.SignalPlot
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.stream.IntStream

class EcgPdfReportDetailsBuilder constructor(
    private val patientInfo: PatientInfo,
    private val ecgData: EcgData,
    private val ecgDiagnosis: EcgDiagnosis,
) {

    fun build(): EcgPdfReportDetails {
        val ecgImage = getEcgVisualization()
        val ecgConclusion = getEcgConclusion()

        return EcgPdfReportDetails.createDefaultEcgReport(patientInfo, ecgImage, EcgLeadType.II, ecgConclusion)
    }

    private fun getEcgConclusion(): String {
        var mostLikelyArrhythmiaType = EcgArrhythmiaType.NOR
        var mostLikelyArrhythmiaNumber = -1
        for (arrhythmia in ecgDiagnosis.diagnosis) {
            if (mostLikelyArrhythmiaNumber < arrhythmia.value) {
                mostLikelyArrhythmiaNumber = arrhythmia.value
                mostLikelyArrhythmiaType = arrhythmia.key
            }
        }
        return String.format("Your ECG contains mostly %s beats", mostLikelyArrhythmiaType.fullName)
    }

    private fun getEcgVisualization(): BufferedImage {

        val ecgSignal = ecgData.attributes.getAttributeValue(EcgAttributes.LEADS_FILTERED)[EcgLeadType.II]
            ?: throw Exception("Filtered ECG signal is not provided for report")

        val x = DoubleArray(ecgSignal.size)
        IntStream.range(0, ecgSignal.size).forEach { x[it] = it.toDouble() + 1 }

        val plot = SignalPlot()

        plot.plot(x, ecgSignal, "-", Color.ORANGE, 1.0f)
        plot.displayGrid(false, false)
        plot.displayAxis(false, false)

        return plot.getBufferedImage(570, 150)

    }

}