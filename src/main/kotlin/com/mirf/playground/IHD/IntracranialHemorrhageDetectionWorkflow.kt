package com.mirf.playground.IHD

import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.property.UnitValue
import com.mirf.core.algorithm.Algorithm
import com.mirf.core.data.MirfData
import com.mirf.core.data.medimage.ImagingData
import com.mirf.core.pipeline.AccumulatorWithAlgBlock
import com.mirf.core.pipeline.AlgorithmHostBlock
import com.mirf.core.pipeline.Pipeline
import com.mirf.features.console.utils.Transp
import com.mirf.features.dicomimage.alg.Windowing
import com.mirf.features.dicomimage.data.DicomAttributeCollection
import com.mirf.features.dicomimage.data.DicomData
import com.mirf.features.dicomimage.data.IHDData
import com.mirf.features.dicomimage.util.DicomReader
import com.mirf.features.pdf.PdfElementsAccumulator
import com.mirf.features.pdf.asPdfElementData
import com.mirf.features.reports.PdfElementData
import com.mirf.features.repository.LocalRepositoryCommander
import com.mirf.features.repositoryaccessors.AlgorithmExecutionException
import com.mirf.features.repositoryaccessors.data.RepoRequest
import com.mirf.playground.IHD.pdf.IhdPdfReportCreator
import com.mirf.playground.IHD.pdf.IhdPdfReportDetails
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import java.time.LocalDateTime
import javax.imageio.ImageIO


class IhdClassifierAlg : Algorithm<List<String>, List<Byte>> {
    override fun execute(input: List<String>): List<Byte> {
        val ihdData = IntracranialHemorrhageDetectionWorkflow.createIHDData(input[0])
        val ctData = IntracranialHemorrhageDetectionWorkflow.createCTData(
            ihdData, ihdData.getImageDataAsIntArray()[0], ihdData.getImageDataAsIntArray()[1]
        )
        val ctDiagnosis = IntracranialHemorrhageDetectionDiagnosis(ctData)

        val classification = ctDiagnosis.classify()
//        val classification = listOf<Float>(0.032059982f, 0.003871989f, 0.0058109034f, 0.0042512226f, 0.0046861684f, 0.00847459f)

        val readableDiagnosis = ctDiagnosis.createHumanReadableConclusion(classification.toFloatArray())


        val pdfDetails = IhdPdfReportDetails.createDefaultIhdReportDetails(ihdData.getImage(), readableDiagnosis)
        val reportCreator = IhdPdfReportCreator(pdfDetails)
        val result = reportCreator.createReport()

        val reportAsBytes: ByteArray = result.stream.toByteArray()
//        val reportAsBytes : ByteArray = PdfElementsAccumulator.createPdfResultStream(pdfElementsCollection)

//


        println("Strated writing to temp file: ${classification.joinToString()}")
        val resultFile = File("result-file.pdf")
        val os = FileOutputStream(resultFile)
        os.write(reportAsBytes)
        os.close()


        return reportAsBytes.toMutableList()
    }
}


class IntracranialHemorrhageDetectionWorkflow(private val pipe: Pipeline) {
    fun exec() {
        pipe.run(RepoRequest("", LocalRepositoryCommander()))
    }

    companion object {
        fun createFull(dicomInputFile: String, workingDirPath: Path): IntracranialHemorrhageDetectionWorkflow {

            val pipe = Pipeline("pipe", LocalDateTime.now(), LocalRepositoryCommander(workingDirPath))

            val ctReader = AlgorithmHostBlock<MirfData, ImagingData<BufferedImage>>(
                { createIHDData(dicomInputFile) },
                name = "DICOM reading",
                pipelineKeeper = pipe
            )

            val cTImageExtractor = AlgorithmHostBlock<ImagingData<BufferedImage>, ImagingData<BufferedImage>>(
                { x ->
                    createCTData(x, x.getImageDataAsIntArray()[0],
                        x.getImageDataAsIntArray()[1])
                },
                pipelineKeeper = pipe,
                name = "CTImage extractor")


            ctReader.dataReady += cTImageExtractor::inputReady

            pipe.session.newRecord += { _, b -> println(b) }

            pipe.rootBlock = ctReader

            return IntracranialHemorrhageDetectionWorkflow(pipe)
        }

        fun createHighlightedImages(imageData: ImagingData<BufferedImage>): PdfElementData {
            val result = Paragraph()
            val image = imageData.getImage()
            try {
                val stream = ByteArrayOutputStream()
                ImageIO.write(image, "jpg", stream)

                val pdfImage = Image(ImageDataFactory.create(stream.toByteArray()))
                pdfImage.width = UnitValue.createPercentValue(50f)
                pdfImage.setMargins(10f, 10f, 10f, 10f)
                pdfImage.setHeight(UnitValue.createPercentValue(50f))

                result.add(pdfImage)
            } catch (e: Exception) {
                throw AlgorithmExecutionException(e)
            }

            return PdfElementData(result)
        }

        fun createIHDData(dicomInputFile: String): IHDData {
            println("START TO READ DICOM FILE $dicomInputFile")
            val list = DicomReader.readDicomImageAttributesFromLocalFile(dicomInputFile)
            val dicomAttributeCollection = DicomAttributeCollection(list)
            println("SUCCESSFULLY READ DICOM!!")
            val ihdData: IHDData?
            try {
                ihdData = IHDData(dicomAttributeCollection)
            } catch (ex: Exception) {
                ex.printStackTrace()
                throw ex
            }
            println("SUCCESSFULLY CREATED IHD DATA!!")
            return ihdData
        }

        fun createCTData(imgData: ImagingData<BufferedImage>, slope: Int, intercept: Int): ImagingData<BufferedImage> {
            val brain = Windowing(slope, intercept).brainWindow(imgData.getImageDataAsFloatArray())
            val subdural = Windowing(slope, intercept).subduralWindow(imgData.getImageDataAsFloatArray())
            val soft = Windowing(slope, intercept).boneWindow(imgData.getImageDataAsFloatArray())
            val mix = Transp.flatten(Transp.transpImage(brain, subdural, soft))
            return DicomData(mix)
        }
    }


}