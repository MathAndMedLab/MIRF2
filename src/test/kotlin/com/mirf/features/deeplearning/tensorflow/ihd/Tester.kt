package com.mirf.features.deeplearning.tensorflow.ihd

import com.mirf.playground.IHD.IntracranialHemorrhageDetectionWorkflow
import java.nio.file.Paths

object Tester {
    @JvmStatic
    fun main(args: Array<String>) {

        val dicomInputFile1 = "/home/alexandra/DISK/MIRF2/src/main/resources/ihd_dicom/ihd_001.dcm"
        //val dicomInputFile2 = "src/main/resources/dicoms/image-000001.dcm"
        //val dicomInputFile = "src/main/resources/dicoms/image-000001.dcm"

        val workflow = IntracranialHemorrhageDetectionWorkflow.createFull(dicomInputFile1,
                Paths.get("/home/alexandra/DISK/MIRF2/workingDir"))
        workflow.exec()

        //val workflow = DicomImageCircleMaskApplier
    }
}