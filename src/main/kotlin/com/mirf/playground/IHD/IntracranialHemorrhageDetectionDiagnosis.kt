package com.mirf.playground.IHD

import com.mirf.core.data.AttributeCollection
import com.mirf.core.data.Data
import com.mirf.core.data.medimage.ImagingData
import com.mirf.features.deeplearning.tensorflow.TensorflowModelInterface
import java.awt.image.BufferedImage

class IntracranialHemorrhageDetectionDiagnosis(image: ImagingData<BufferedImage>) : Data {
    // IntracranialHemorrhageDetection
    var image: ImagingData<BufferedImage> = image
        private set
    var modelName = "dicom_classifier/model_17_03_2021.pb"
        private set
    var inputName = "input_1"
        private set
    var outputName = "dense_output/Sigmoid"
        private set
    var dims: Long = 512
        private set

    val diagnosisNames = listOf("Epidural hemorrhage",
        "Intraparenchymal hemorrhage",
        "Intraventricular hemorrhage",
        "Subarachnoid hemorrhage",
        "Subdural hemorrhage")

    private fun forwardPass(): FloatArray {
        val tfModel = TensorflowModelInterface(null, modelName, inputName, outputName, 6)

        val image1 = image.getImageDataAsFloatArray()
        return tfModel.runModel(image1, 1, dims, dims, 3)
    }

    fun classify(): List<Float> {
        val answers: FloatArray = forwardPass()
        return answers.asList()
    }

    fun diagnose(): FloatArray {
        return forwardPass()
    }

    fun createHumanReadableConclusion(cnnOutput: FloatArray): String {
        val conclusion: String

        var maxIndex = 0
        var maxVal = cnnOutput[0]
        cnnOutput.forEachIndexed { ind, value ->
            if (value.compareTo(maxVal) > 0) {
                maxIndex = ind
                maxVal = value
            }
        }

        conclusion = if (maxIndex == 5) {
            "Most likely this image does not contain any hemorrhage\n" + cnnOutput.joinToString()
        } else {
            "The most likely diagnosis is ${diagnosisNames[maxIndex]} \n" + cnnOutput.joinToString()
        }

        return conclusion
    }

    override val attributes: AttributeCollection
        get() = image.attributes
}