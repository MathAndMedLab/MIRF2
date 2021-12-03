package com.mirf.playground.IHD.pdf

import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import com.itextpdf.layout.property.VerticalAlignment
import com.mirf.features.pdf.PdfDocumentInfo
import com.mirf.features.pdf.asPdfImage
import java.io.ByteArrayOutputStream

class IhdPdfReportCreator(private val details: IhdPdfReportDetails) {

    private val title = "Intracranial hemorrhage detection \n AI-supported report"

    private val marginBlock = Paragraph().setMarginBottom(10f)

    fun createReport(): PdfDocumentInfo {
        val resultStream = ByteArrayOutputStream()
        val writer = PdfWriter(resultStream)
        val pdf = PdfDocument(writer)

        val document = Document(pdf, PageSize.A4)

        document.setMargins(10f, 10f, 10f, 10f)
        document.add(createHeader())
        document.add(marginBlock)
        document.add(marginBlock)
        document.add(createIhdImage())
        document.add(marginBlock)
        document.add(createConclusion())

        document.close()
        return PdfDocumentInfo(pdf, resultStream)
    }

    private fun createHeader(): Paragraph {

        val header = Paragraph()
        header.setTextAlignment(TextAlignment.CENTER)

        header.width = UnitValue.createPercentValue(100f)
        header.setVerticalAlignment(VerticalAlignment.MIDDLE)

        val logo: Image = details.companyLogo.asPdfImage()
        logo.setRelativePosition(70f, 0f, 0f, 5f)

        val titleText = Text(title).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(18f)
        val title = Paragraph(titleText).setRelativePosition(logo.width.value / 2, 0f, 0f, 0f)
        header.add(title)
        header.add(logo)

        return header
    }

    private fun createIhdImage(): Paragraph {
        val result = Paragraph()

        result.width = UnitValue.createPercentValue(100f)
        result.setVerticalAlignment(VerticalAlignment.MIDDLE)

        val ecgImage: Image = details.dicomImage.asPdfImage()
        ecgImage.setRelativePosition(30f, 0f, 0f, 20f)

        result.add(ecgImage)

        return result
    }

    private fun createConclusion(): Paragraph {
        return Paragraph("Conclusion: " + details.reportConclusion).setFontSize(16f)
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMargins(0f, 5f, 0f, 5f)
    }
}