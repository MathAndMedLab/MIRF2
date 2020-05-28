package com.mirf.features.ecg.util

import com.mirf.core.algorithm.Algorithm
import com.mirf.core.data.CollectionData
import com.mirf.core.data.Data
import com.mirf.core.data.FileData
import com.mirf.core.data.MirfData
import com.mirf.core.data.medimage.BufferedImageSeries
import com.mirf.core.pipeline.AlgorithmHostBlock
import com.mirf.core.pipeline.Pipeline
import com.mirf.features.ecg.EcgData
import com.mirf.features.ecg.EcgLeadType
import com.mirf.features.ecg.EcgReader
import com.mirf.features.ecg.reportpdf.EcgPdfReportCreator
import com.mirf.features.ecg.reportpdf.EcgPdfReportDetailsBuilder
import com.mirf.features.ecg.reportpdf.EcgReportBuilderBlock
import com.mirf.features.repository.LocalRepositoryCommander
import com.mirf.features.repositoryaccessors.RepoFileSaver
import com.mirf.features.repositoryaccessors.RepositoryAccessorBlock
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime

class EcgReaderAlg: Algorithm<List<String>, EcgData> {
    override fun execute(input: List<String>): EcgData {
        if (input.size != 2) {
            throw IllegalArgumentException("Invalid input params. Expected List of two Strings.")
        }

        val headerPath = input[1]
        val dataPath = input[0]
        return EcgReader.readEcg(headerPath, dataPath, 212)
    }
}

class EcgCleanerAlg: Algorithm<EcgData, EcgData> {
    override fun execute(input: EcgData): EcgData {
        return EcgCleaner.filterNoises(input, EcgLeadType.II)
    }
}

class EcgBeatExtractorAlg: Algorithm<EcgData, BufferedImageSeries> {
    override fun execute(input: EcgData): BufferedImageSeries {
        return EcgBeatExtractor.extractBeatImages(input ,EcgLeadType.II)
    }
}

class EcgClassifierAlg: Algorithm<BufferedImageSeries, EcgDiagnosis> {
    override fun execute(input: BufferedImageSeries): EcgDiagnosis {
        return EcgClassifier.classify(input)
    }
}

class CollectionDataAlg: Algorithm<CollectionData<Data>, FileData> {
    override fun execute(input: CollectionData<Data>): FileData {
        val it = input.collection.iterator()
        val ecgFiltered = it.next() as EcgData
        val ecgDiagnosis = it.next() as EcgDiagnosis

        val patientInfo = PatientInfo("Leslie", 74, "W", LocalDateTime.now())

        val reportDetails = EcgPdfReportDetailsBuilder(patientInfo, ecgFiltered, ecgDiagnosis).build()
        val ecgReport = EcgPdfReportCreator(reportDetails).createReport()

        return FileData(ecgReport.stream.toByteArray(), "patientEcgReport", ".pdf")
    }
}

class EcgClassificationWorkflow(val pipe: Pipeline) {

    fun exec() {
        pipe.run(MirfData.empty)
    }

    companion object {
        fun createFull(dataPath: String,
                     headerPath: String,
                     workingDir:String,
                     patientInfo : PatientInfo) : EcgClassificationWorkflow {

            val workingDirPath = Paths.get(workingDir).resolve("${patientInfo.name}_${LocalDate.now()}".replace(" ", "_"))
            val pipe = Pipeline("pipe", LocalDateTime.now(), LocalRepositoryCommander(workingDirPath))

            val ecgReader = AlgorithmHostBlock<MirfData, EcgData>(
                    {x ->  EcgReader.readEcg(headerPath, dataPath, 212)},
                    pipelineKeeper = pipe,
                    name = "ECG reading")

            val ecgCleaner = AlgorithmHostBlock<EcgData, EcgData>(
                    {x ->  EcgCleaner.filterNoises(x, EcgLeadType.II)},
                    pipelineKeeper = pipe,
                    name = "ECG filtering")

            val ecgBeatExtractor = AlgorithmHostBlock<EcgData, BufferedImageSeries>(
                    {EcgBeatExtractor.extractBeatImages(it ,EcgLeadType.II)},
                    pipelineKeeper = pipe,
                    name = "Beat extractor")

            val ecgClassifier = AlgorithmHostBlock<BufferedImageSeries, EcgDiagnosis>(
                    {EcgClassifier.classify(it)},
                    pipelineKeeper = pipe,
                    name = "Classifier"
            )

            val reportBuilderBlock = EcgReportBuilderBlock(patientInfo, pipe)
            val reportSaverBlock = RepositoryAccessorBlock<FileData, Data>(pipe.repositoryCommander, RepoFileSaver(), "")

            reportBuilderBlock.setEcgDiagnosis(ecgClassifier)
            reportBuilderBlock.setEcgSignal(ecgCleaner)

            ecgReader.dataReady += ecgBeatExtractor::inputReady
            ecgReader.dataReady += ecgCleaner::inputReady
            ecgBeatExtractor.dataReady += ecgClassifier::inputReady
            reportBuilderBlock.dataReady += reportSaverBlock::inputReady

            pipe.rootBlock = ecgReader

            return EcgClassificationWorkflow(pipe)
        }
    }
}