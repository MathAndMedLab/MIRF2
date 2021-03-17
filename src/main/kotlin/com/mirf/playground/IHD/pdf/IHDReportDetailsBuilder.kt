package com.mirf.playground.IHD.pdf

import com.mirf.features.dicomimage.data.DicomData
import com.mirf.playground.IHD.IntracranialHemorrhageDetectionDiagnosis
import java.awt.Color
import java.awt.image.BufferedImage
import java.lang.Exception
import java.lang.Integer.max
import java.util.ArrayList
import java.util.stream.IntStream

class IHDReportDetailsBuilder constructor(
        private val patientInfo: DicomData,
        private val ihdDiagnosis: IntracranialHemorrhageDetectionDiagnosis) {

    fun build(): IHDReportDetails {
        val image = patientInfo.getImage()

        return IHDReportDetails.createDefaultIHDReport(image)
    }

    private fun getIHDConclusion() : String{
        return String.format("ihd ", ihdDiagnosis.diagnose().asList())
    }

    private fun getIHDVisualization() :BufferedImage {

        return patientInfo.getImage()

    }

}