package com.mirf.playground

import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.io.source.ByteArrayOutputStream
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.property.UnitValue
import com.mirf.core.algorithm.Algorithm
import com.mirf.core.algorithm.asImageSeriesAlg
import com.mirf.core.data.CollectionData
import com.mirf.core.data.Data
import com.mirf.core.data.FileData
import com.mirf.core.data.medimage.ImageSeries
import com.mirf.core.data.medimage.getImageWithHighlightedSegmentation
import com.mirf.core.pipeline.AccumulatorWithAlgBlock
import com.mirf.core.pipeline.AlgorithmHostBlock
import com.mirf.core.pipeline.Pipeline
import com.mirf.features.dicomimage.util.DicomRepoRequestProcessors
import com.mirf.features.pdf.PdfElementsAccumulator
import com.mirf.features.pdf.asPdfElementData
import com.mirf.features.reports.PdfElementData
import com.mirf.features.repository.LocalRepositoryCommander
import com.mirf.features.repositoryaccessors.AlgorithmExecutionException
import com.mirf.features.repositoryaccessors.RepoFileSaver
import com.mirf.features.repositoryaccessors.RepositoryAccessorBlock
import com.mirf.features.repositoryaccessors.data.RepoRequest
import java.nio.file.Paths
import javax.imageio.ImageIO

class DicomImageSeriesReaderAlg : Algorithm<List<String>, ImageSeries> {

    init {
        println("Should create a dir here!!!!!!!!!")
        LocalRepositoryCommander(Paths.get("workingdir"));
    }

    override fun execute(input: List<String>): ImageSeries {
        if (input.isEmpty()) {
            throw IllegalArgumentException("Invalid input params. Link should be specified")
        }
        val repoRequest = RepoRequest(input[0], LocalRepositoryCommander())
        return DicomRepoRequestProcessors.readDicomImageSeries(repoRequest)
    }
}

class DicomAddCircleMaskAlg : Algorithm<ImageSeries, ImageSeries> {
    override fun execute(input: ImageSeries): ImageSeries {
        return AddCircleMaskAlg().asImageSeriesAlg().execute(input)
    }
}

class ConvertHighlightedDicomImagesToPdfAlg : Algorithm<ImageSeries, PdfElementData> {
    override fun execute(input: ImageSeries): PdfElementData {
        return DicomImageCircleMaskApplier.createHighlightedImages(input)
    }
}

class ConvertDicomImagesToPdfAlg : Algorithm<ImageSeries, PdfElementData> {
    override fun execute(input: ImageSeries): PdfElementData {
        return input.asPdfElementData()
    }
}

class PdfFileCreatorAlg : Algorithm<CollectionData<Data>, List<Byte>> {
    override fun execute(input: CollectionData<Data>): List<Byte> {
        val it = input.collection.iterator()
        val beforeMask = it.next() as PdfElementData
        val afterMask = it.next() as PdfElementData

        val pdfElementsCollection = input as CollectionData<PdfElementData>

        val reportAsBytes: ByteArray = PdfElementsAccumulator.createPdfResultStream(pdfElementsCollection)

        return reportAsBytes.toMutableList()
    }
}


class PipelineForDeveloping : Algorithm<List<String>, List<Byte>> {
    override fun execute(input: List<String>): List<Byte> {
        if (input.isEmpty()) {
            throw IllegalArgumentException("Invalid input params. Link should be specified")
        }
        val repoRequest = RepoRequest(input[0], LocalRepositoryCommander())
        val series = DicomRepoRequestProcessors.readDicomImageSeries(repoRequest)
        val seriesWithMask = AddCircleMaskAlg().asImageSeriesAlg().execute(series)

        val imagesWithMaskPdfElement = DicomImageCircleMaskApplier.createHighlightedImages(seriesWithMask)
        val initialImagesPdfElemnt = series.asPdfElementData()

        val collection: Collection<PdfElementData> = listOf(initialImagesPdfElemnt, imagesWithMaskPdfElement)

        val pdfElementsCollection = CollectionData(collection)

        val reportAsBytes: ByteArray = PdfElementsAccumulator.createPdfResultStream(pdfElementsCollection)

        return reportAsBytes.toMutableList()
    }
}


class DicomImageCircleMaskApplier {

    fun exec(dicomFolderLink: String, resultFolderLink: String) {
        val pipe = Pipeline("apply circle mask to dicom")

        //initializing blocks
        val seriesReaderBlock = AlgorithmHostBlock(
            DicomRepoRequestProcessors.readDicomImageSeriesAlg,
            pipelineKeeper = pipe)

        val addMaskBlock = AlgorithmHostBlock(
            AddCircleMaskAlg().asImageSeriesAlg(),
            pipelineKeeper = pipe)

        val imageBeforeReporter = AlgorithmHostBlock<ImageSeries, PdfElementData>(
            { x -> x.asPdfElementData() },
            "image before", pipe)

        val imageAfterReporter = AlgorithmHostBlock<ImageSeries, PdfElementData>(
            { x -> createHighlightedImages(x) },
            "image after", pipe)

        val pdfBlock = AccumulatorWithAlgBlock(
            PdfElementsAccumulator("report"),
            2,
            "Accumulator",
            pipe)

        val reportSaverBlock = RepositoryAccessorBlock<FileData, Data>(LocalRepositoryCommander(),
            RepoFileSaver(), resultFolderLink)

        //making connections
        seriesReaderBlock.dataReady += addMaskBlock::inputReady
        seriesReaderBlock.dataReady += imageBeforeReporter::inputReady

        addMaskBlock.dataReady += imageAfterReporter::inputReady

        imageBeforeReporter.dataReady += pdfBlock::inputReady
        imageAfterReporter.dataReady += pdfBlock::inputReady

        pdfBlock.dataReady += reportSaverBlock::inputReady

        //create initial data
        val init = RepoRequest(dicomFolderLink, LocalRepositoryCommander())

        //print every new session record
        pipe.session.newRecord += { _, b -> println(b) }

        //run
        pipe.rootBlock = seriesReaderBlock
        pipe.run(init)
    }

    companion object {
        fun createHighlightedImages(series: ImageSeries): PdfElementData {
            val images = series.images.stream().map { x -> x.getImageWithHighlightedSegmentation() }
            val result = Paragraph()
            try {
                for (image in images) {
                    val stream = ByteArrayOutputStream()
                    ImageIO.write(image, "jpg", stream)

                    val pdfImage = Image(ImageDataFactory.create(stream.toByteArray()))
                    pdfImage.width = UnitValue.createPercentValue(50f)
                    pdfImage.setMargins(10f, 10f, 10f, 10f)
                    pdfImage.setHeight(UnitValue.createPercentValue(50f))

                    result.add(pdfImage)
                }
            } catch (e: Exception) {
                throw AlgorithmExecutionException(e)
            }

            return PdfElementData(result)
        }
    }
}


