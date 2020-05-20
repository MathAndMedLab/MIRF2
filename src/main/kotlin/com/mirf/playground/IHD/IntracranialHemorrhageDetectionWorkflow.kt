package com.mirf.playground.IHD

import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.property.UnitValue
import com.mirf.core.data.Data
import com.mirf.core.data.FileData
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
import com.mirf.features.reports.PdfElementData
import com.mirf.features.repository.LocalRepositoryCommander
import com.mirf.features.repositoryaccessors.AlgorithmExecutionException
import com.mirf.features.repositoryaccessors.RepoFileSaver
import com.mirf.features.repositoryaccessors.RepositoryAccessorBlock
import com.mirf.features.repositoryaccessors.data.RepoRequest
import com.mirf.playground.IHD.pdf.IHDReportBuilderBlock
import com.pixelmed.dicom.TagFromName
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.time.LocalDateTime
import javax.imageio.ImageIO

class IntracranialHemorrhageDetectionWorkflow(val pipe: Pipeline) {
    fun exec() {
        pipe.run(RepoRequest("", LocalRepositoryCommander()))
    }
    companion object {
        fun createFull(dicomInputFile: String, workingDirPath: Path) : IntracranialHemorrhageDetectionWorkflow {

            val pipe = Pipeline("pipe", LocalDateTime.now(), LocalRepositoryCommander(workingDirPath))
            //val slope : Int = Integer.parseInt(dicomData.getAttributeValue(TagFromName.RescaleSlope))
            //val intercept : Int = Integer.parseInt(dicomData.getAttributeValue(TagFromName.RescaleIntercept))


            val ctReader = AlgorithmHostBlock<MirfData, ImagingData<BufferedImage>>(
                    {x -> createIHDData(dicomInputFile)},
                    name = "DICOM reading",
                    pipelineKeeper = pipe
            )

            val CTImageExtractor = AlgorithmHostBlock<ImagingData<BufferedImage>, ImagingData<BufferedImage>>(
                    {x ->
                        createCTData(x, x.getImageDataAsIntArray()[0],
                                x.getImageDataAsIntArray()[1])
                    },
                    pipelineKeeper = pipe,
                    name = "CTImage extractor")

            val classifier = AlgorithmHostBlock<ImagingData<BufferedImage>, IntracranialHemorrhageDetectionDiagnosis>(
                    {x -> val ctDiagnosis = IntracranialHemorrhageDetectionDiagnosis(x)
                        ctDiagnosis.classify();
                        ctDiagnosis
                    },
                    pipelineKeeper = pipe,
                    name = "Classifier"
            )

/*
            val imageReporter = AlgorithmHostBlock<ImagingData<BufferedImage>, PdfElementData>(
                    { x -> createHighlightedImages(x) },
                    "image before", pipe)

            val pdfBlock = AccumulatorWithAlgBlock(PdfElementsAccumulator(
                    "report"),
                    1,
                    "Accumulator",
                    pipe)

            val reportSaverBlock = RepositoryAccessorBlock<FileData, Data>(LocalRepositoryCommander(),
                    RepoFileSaver(), "")*/



            //ctReader.dataReady += imageReporter::inputReady

            ctReader.dataReady += CTImageExtractor::inputReady
            CTImageExtractor.dataReady += classifier::inputReady

            //pdfBlock.dataReady += reportSaverBlock::inputReady

            //imageReporter.dataReady += pdfBlock::inputReady
            //pdfBlock.dataReady += reportSaverBlock::inputReady

            pipe.session.newRecord += { _, b -> println(b) }

            pipe.rootBlock = ctReader

            return IntracranialHemorrhageDetectionWorkflow(pipe)
        }

        fun createHighlightedImages(imageData: ImagingData<BufferedImage>) : PdfElementData {
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
            }
            catch (e: Exception) {
                throw AlgorithmExecutionException(e)
            }

            return PdfElementData(result)
        }

        fun createIHDData(dicomInputFile: String): IHDData {
            val list = DicomReader.readDicomImageAttributesFromLocalFile(dicomInputFile)
            val dicomAttributeCollection = DicomAttributeCollection(list)
            return IHDData(dicomAttributeCollection)
        }

        fun createCTData(imgData: ImagingData<BufferedImage>, slope : Int, intercept : Int): ImagingData<BufferedImage> {
            val brain = Windowing(slope, intercept).brainWindow(imgData.getImageDataAsFloatArray())
            val subdural = Windowing(slope, intercept).subduralWindow(imgData.getImageDataAsFloatArray())
            val soft = Windowing(slope, intercept).boneWindow(imgData.getImageDataAsFloatArray())
            val mix = Transp.flatten(Transp.transp_image(brain, subdural, soft))
            return DicomData(mix)
        }
    }


}