package com.mirf.features.ecg.reportpdf

import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import com.itextpdf.layout.property.VerticalAlignment
import com.mirf.features.pdf.PdfDocumentInfo
import com.mirf.features.pdf.asPdfImage
import java.io.ByteArrayOutputStream

class EcgPdfReportCreator(private val details: EcgPdfReportDetails) {

    private val TITLE = "Electrocardiogram AI-supported report"

    private val marginBlock = Paragraph().setMarginBottom(10f)

    fun createReport(): PdfDocumentInfo {
        val resultStream = ByteArrayOutputStream()
        val writer = PdfWriter(resultStream)
        val pdf = PdfDocument(writer)

        val document = Document(pdf, PageSize.A4)

        document.setMargins(10f, 10f, 10f, 10f)
        document.add(createHeader())
        document.add(marginBlock)
        document.add(createPatientInfoTable())
        document.add(marginBlock)
        document.add(createEcgImage())
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

        val titleText = Text(TITLE).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(18f)
        val title = Paragraph(titleText).setRelativePosition(logo.width.value / 2, 0f, 0f, 0f)
        header.add(title)
        header.add(logo)

        return header
    }

    private fun createPatientInfoTable(): Table {

        val result = Table(arrayOf(UnitValue.createPercentValue(30f), UnitValue.createPercentValue(70f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMargins(0f, 5f, 0f, 5f)

        val patientNameRow = Text("Patient's name:")
        patientNameRow.setFontSize(14f)
        patientNameRow.setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
        patientNameRow.setFontColor(ColorConstants.GRAY)

        val patientAgeRow = Text("Patient's age:")
        patientAgeRow.setFontSize(14f)
        patientAgeRow.setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
        patientAgeRow.setFontColor(ColorConstants.GRAY)

        val patientDateRow = Text("Date of ECG recording:")
        patientDateRow.setFontSize(14f)
        patientDateRow.setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
        patientDateRow.setFontColor(ColorConstants.GRAY)

        val patientRowsParagraph = Paragraph()
            .add(patientNameRow).add("\n")
            .add(patientAgeRow).add("\n")
            .add(patientDateRow)
        patientRowsParagraph.setHorizontalAlignment(HorizontalAlignment.LEFT)


        val rowNamesCell = Cell().add(patientRowsParagraph).setBorder(Border.NO_BORDER)

        result.addCell(rowNamesCell)

        val patientName = Text(details.patientInfo.name)
        patientName.setFontSize(14f)
        patientName.setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))

        val patientAge = Text(details.patientInfo.age.toString())
        patientName.setFontSize(16f)
        patientName.setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))

        val dateOfRecording = Text(details.patientInfo.dateOfEcg.toLocalDate().toString())
        dateOfRecording.setFontSize(14f)
        dateOfRecording.setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))


        val valuesParagraph = Paragraph().add(patientName).add("\n").add(patientAge).add("\n").add(dateOfRecording)
        valuesParagraph.setHorizontalAlignment(HorizontalAlignment.LEFT)

        val rowValuesCell = Cell().add(valuesParagraph).setBorder(Border.NO_BORDER)

        result.addCell(rowValuesCell)

        return result
    }

    private fun createEcgImage(): Paragraph {
        val result = Paragraph()

        result.width = UnitValue.createPercentValue(100f)
        result.setVerticalAlignment(VerticalAlignment.MIDDLE)

        val ecgImage: Image = details.ecgImage.asPdfImage()
        ecgImage.setRelativePosition(5f, 0f, 0f, 5f)

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