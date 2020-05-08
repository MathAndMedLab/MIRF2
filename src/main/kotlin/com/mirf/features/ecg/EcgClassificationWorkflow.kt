package com.mirf.features.ecg.util

import com.mirf.core.data.Data
import com.mirf.core.data.FileData
import com.mirf.core.data.MirfData
import com.mirf.core.data.medimage.BufferedImageSeries
import com.mirf.core.pipeline.AlgorithmHostBlock
import com.mirf.core.pipeline.Pipeline
import com.mirf.features.ecg.EcgData
import com.mirf.features.ecg.EcgLeadType
import com.mirf.features.ecg.EcgReader
import com.mirf.features.ecg.reportpdf.EcgReportBuilderBlock
import com.mirf.features.repository.LocalRepositoryCommander
import com.mirf.features.repositoryaccessors.RepoFileSaver
import com.mirf.features.repositoryaccessors.RepositoryAccessorBlock
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime

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