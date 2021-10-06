package com.mirf.playground.IHD

import com.mirf.core.data.AttributeCollection
import com.mirf.core.data.Data
import com.mirf.core.data.medimage.ImagingData
import com.mirf.features.deeplearning.tensorflow.TensorflowModelInterface
import java.awt.image.BufferedImage

class IntracranialHemorrhageDetectionDiagnosis : Data {
    // IntracranialHemorrhageDetection
    lateinit var image : ImagingData<BufferedImage>
        private set
    var modelName  = "dicom_classifier/model_17_03_2021.pb"
        private set
    var inputName = "input_1"
        private set
    var outputName = "dense_output/Sigmoid"
        private set
    var dims : Long = 512
        private set

    val diagnosisNames = listOf<String>("Epidural hemorrhage",
        "Intraparenchymal hemorrhage",
        "Intraventricular hemorrhage",
        "Subarachnoid hemorrhage",
        "Subdural hemorrhage")

    constructor(image: ImagingData<BufferedImage>) {
        this.image = image
    }

    private fun forward_pass(): FloatArray {
        val tfModel = TensorflowModelInterface(null, modelName, inputName, outputName, 6)

        val image1 = image.getImageDataAsFloatArray()
        return tfModel.runModel(image1, 1, dims, dims, 3)
    }

    fun classify() : List<Float> {
        val answers : FloatArray = forward_pass()
        return answers.asList()
    }

    fun diagnose() : FloatArray {
        return forward_pass()
    }

    fun createHumanReadableConclusion(cnnOutput: FloatArray) : String {
        var conclusion = "";

        var maxIndex = 0
        var maxVal = cnnOutput.get(0)
        cnnOutput.forEachIndexed{ ind, value ->
            if (value.compareTo(maxVal) > 0) {
                maxIndex = ind
                maxVal = value
            }
        }

        if (maxIndex.equals(5)) {
            conclusion = "Most likely this image does not contain any hemorrhage\n" + cnnOutput.joinToString()
        } else {
            conclusion = "The most likely diagnosis is ${diagnosisNames.get(maxIndex)} \n" + cnnOutput.joinToString()
        }

        return conclusion
    }

    override val attributes: AttributeCollection
        get() = image.attributes
}