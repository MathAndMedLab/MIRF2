package com.mirf.features.deeplearning.tensorflow.ihd

import com.mirf.playground.IHD.IntracranialHemorrhageDetectionWorkflow
import java.nio.file.Paths

object Tester {
    @JvmStatic
    fun main(args: Array<String>) {

        val dicomInputFile1 = "/Users/alexander.savelyev/IdeaProjects/MIRF2/src/main/resources/dicoms/ID_000a2d7b0.dcm"
        //val dicomInputFile2 = "src/main/resources/dicoms/image-000001.dcm"
        //val dicomInputFile = "src/main/resources/dicoms/image-000001.dcm"




        val workflow = IntracranialHemorrhageDetectionWorkflow.createFull(dicomInputFile1,
                Paths.get("/Users/alexander.savelyev/IdeaProjects/test_pixelmed/workingDir"))
        workflow.exec()



        //val workflow = DicomImageCircleMaskApplier
    }
}