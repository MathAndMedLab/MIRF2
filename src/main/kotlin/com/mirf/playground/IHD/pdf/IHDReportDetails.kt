package com.mirf.playground.IHD.pdf

import java.awt.image.BufferedImage
import java.time.LocalDateTime
import javax.imageio.ImageIO

class IHDReportDetails(val companyLogo: BufferedImage,
                          val reportCreationTime: LocalDateTime,
                          //val patientInfo: PatientInfo,
                          val IHDImage: BufferedImage,
                          val footerIncluded: Boolean)
{
    companion object {
        fun createDefaultIHDReport(//patientInfo: PatientInfo,
                                    iHDImage:BufferedImage
                                   //ecgLeadType: EcgLeadType,
                                   //reportConclusion: String
                                    ) : IHDReportDetails{
            val logoPath = javaClass.getResource("/images/mirf.png")
            val mirfLogo = ImageIO.read(logoPath)
            return IHDReportDetails(
                    mirfLogo,
                    LocalDateTime.now(),
                    //patientInfo,
                    iHDImage,
                    footerIncluded = true
            )
        }
    }
}