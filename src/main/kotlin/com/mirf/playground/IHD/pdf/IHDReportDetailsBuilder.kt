package com.mirf.playground.IHD.pdf

import com.mirf.features.dicomimage.data.DicomData
import com.mirf.playground.IHD.IntracranialHemorrhageDetectionDiagnosis
import java.awt.image.BufferedImage

class IHDReportDetailsBuilder constructor(
    private val patientInfo: DicomData,
    private val ihdDiagnosis: IntracranialHemorrhageDetectionDiagnosis,
) {

    fun build(): IHDReportDetails {
        val image = patientInfo.getImage()

        return IHDReportDetails.createDefaultIHDReport(image)
    }

    private fun getIHDConclusion(): String {
        return String.format("ihd ", ihdDiagnosis.diagnose().asList())
    }

    private fun getIHDVisualization(): BufferedImage {

        return patientInfo.getImage()

    }

}