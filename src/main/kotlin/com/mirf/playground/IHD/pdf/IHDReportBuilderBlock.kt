package com.mirf.playground.IHD.pdf

import com.mirf.core.data.Data
import com.mirf.core.data.FileData
import com.mirf.core.pipeline.PipelineBlock
import com.mirf.core.pipeline.PipelineKeeper
import com.mirf.features.dicomimage.data.DicomData
import com.mirf.playground.IHD.IntracranialHemorrhageDetectionDiagnosis

class IHDReportBuilderBlock(pipelineKeeper: PipelineKeeper) :
    PipelineBlock<Data, FileData>("IHDPdfCreator", pipelineKeeper) {

    private var dicomDataSender: Any? = null
    private var ihdDiagnosisSender: Any? = null

    private var dicomDataSet = false
    private var ihdDiagnosisSet = false

    private var dicomData: DicomData? = null
    private var ihdDiagnosis: IntracranialHemorrhageDetectionDiagnosis? = null

    override fun flush() {
        flushDataSignal()
        flushDataDiagnosis()
    }

    private fun flushDataSignal() {
        dicomData = null
        dicomDataSet = false
        dicomDataSender = null
    }

    private fun flushDataDiagnosis() {
        ihdDiagnosis = null
        ihdDiagnosisSet = false
        ihdDiagnosisSender = null
    }

    fun setDicomData(sender: PipelineBlock<*, *>) {
        flushDataSignal()
        this.dicomDataSender = sender
        sender.dataReady += this::inputReady
    }

    fun setIHDDiagnosis(sender: PipelineBlock<*, *>) {
        flushDataDiagnosis()
        this.ihdDiagnosisSender = sender
        sender.dataReady += this::inputReady
    }

    override fun inputReady(sender: Any, input: Data) {
        when (sender) {
            dicomDataSender -> {
                dicomData = input as DicomData
                dicomDataSet = true
            }
            ihdDiagnosisSender -> {
                ihdDiagnosis = input as IntracranialHemorrhageDetectionDiagnosis
                ihdDiagnosisSet = true
            }
            else -> log.warn("[$name] undefined sender signal received from $sender, ignored")
        }

        if (ihdDiagnosisSet && dicomDataSet) {
            val record = pipelineKeeper.session.addNew("[$name]: algorithm execution")

            val reportDetails = IHDReportDetailsBuilder(dicomData!!, ihdDiagnosis!!).build()
            val ecgReport = IHDReportCreator(reportDetails).createReport()

            val fileData = FileData(ecgReport.stream.toByteArray(), "patientEcgReport", ".pdf")

            onDataReady(this, fileData)

            record.setSuccess()

        }
    }

}