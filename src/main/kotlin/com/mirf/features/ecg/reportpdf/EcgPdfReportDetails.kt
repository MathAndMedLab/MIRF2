package com.mirf.features.ecg.reportpdf

import com.mirf.features.ecg.data.EcgLeadType
import com.mirf.features.ecg.util.PatientInfo
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import javax.imageio.ImageIO

class EcgPdfReportDetails(
    val companyLogo: BufferedImage,
    val reportCreationTime: LocalDateTime,
    val patientInfo: PatientInfo,
    val ecgImage: BufferedImage,
    val ecgLeadType: EcgLeadType,
    val reportConclusion: String,
    val footerIncluded: Boolean,
) {
    companion object {
        fun createDefaultEcgReport(
            patientInfo: PatientInfo,
            ecgImage: BufferedImage,
            ecgLeadType: EcgLeadType,
            reportConclusion: String,
        ): EcgPdfReportDetails {
            val logoPath = javaClass.getResource("/images/mirf.png")
            val mirfLogo = ImageIO.read(logoPath)
            return EcgPdfReportDetails(
                mirfLogo,
                LocalDateTime.now(),
                patientInfo,
                ecgImage,
                ecgLeadType,
                reportConclusion,
                footerIncluded = true
            )
        }
    }
}