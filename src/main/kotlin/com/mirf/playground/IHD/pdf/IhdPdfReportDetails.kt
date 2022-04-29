package com.mirf.playground.IHD.pdf

import java.awt.image.BufferedImage
import java.time.LocalDateTime
import javax.imageio.ImageIO

class IhdPdfReportDetails(
    val companyLogo: BufferedImage,
    val reportCreationTime: LocalDateTime,
    val dicomImage: BufferedImage,
    val reportConclusion: String,
    val footerIncluded: Boolean,
) {
    companion object {
        fun createDefaultIhdReportDetails(ihdImage: BufferedImage, reportConclusion: String): IhdPdfReportDetails {
            val logoPath = javaClass.getResource("/images/mirf.png")
            val mirfLogo = ImageIO.read(logoPath)
            return IhdPdfReportDetails(
                mirfLogo,
                LocalDateTime.now(),
                ihdImage,
                reportConclusion,
                footerIncluded = true
            )
        }
    }
}