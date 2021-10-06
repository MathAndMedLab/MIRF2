package com.mirf.features.ecg.reportpdf

import com.mirf.core.data.Data
import com.mirf.core.data.FileData
import com.mirf.core.pipeline.PipelineBlock
import com.mirf.core.pipeline.PipelineKeeper
import com.mirf.features.ecg.EcgData
import com.mirf.features.ecg.util.EcgDiagnosis
import com.mirf.features.ecg.util.PatientInfo
import java.lang.Exception

class EcgReportBuilderBlock(val patientInfo: PatientInfo, pipelineKeeper: PipelineKeeper) : PipelineBlock<Data, FileData>("ecgPdfCreator", pipelineKeeper) {

    private var ecgFilteredSender: Any? = null
    private var ecgDiagnosisSender: Any? = null

    private var ecgFilteredSet = false
    private var ecgDiagnosisSet = false

    private var ecgFiltered: EcgData? = null
    private var ecgDiagnosis: EcgDiagnosis? = null

    override fun flush() {
        flushEcgSignal()
        flushEcgDiagnosis()
    }

    private fun flushEcgSignal() {
        ecgFiltered = null
        ecgFilteredSet = false
        ecgFilteredSender = null
    }

    private fun flushEcgDiagnosis() {
        ecgDiagnosis = null
        ecgDiagnosisSet = false
        ecgDiagnosisSender = null
    }

    fun setEcgSignal(sender: PipelineBlock<*, *>) {
        flushEcgSignal()
        this.ecgFilteredSender = sender
        sender.dataReady += this::inputReady
    }

    fun setEcgDiagnosis(sender: PipelineBlock<*, *>) {
        flushEcgDiagnosis()
        this.ecgDiagnosisSender = sender
        sender.dataReady += this::inputReady
    }

    override fun inputReady(sender: Any, input: Data) {
        when (sender) {
            ecgFilteredSender -> {
                ecgFiltered = input as EcgData
                ecgFilteredSet = true
            }
            ecgDiagnosisSender -> {
                ecgDiagnosis = input as EcgDiagnosis
                ecgDiagnosisSet = true
            }
            else -> log.warn("[$name] undefined sender signal received from $sender, ignored")
        }

        if (ecgDiagnosisSet && ecgFilteredSet) {
            val record = pipelineKeeper.session.addNew("[$name]: algorithm execution")

            val reportDetails = EcgPdfReportDetailsBuilder(patientInfo, ecgFiltered!!, ecgDiagnosis!!).build()
            val ecgReport = EcgPdfReportCreator(reportDetails).createReport()

            val fileData = FileData(ecgReport.stream.toByteArray(), "patientEcgReport", ".pdf")

            onDataReady(this, fileData)

            record.setSuccess()

        }
    }

}