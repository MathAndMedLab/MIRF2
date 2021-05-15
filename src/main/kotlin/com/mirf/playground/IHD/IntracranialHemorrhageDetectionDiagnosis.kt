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

    constructor(image: ImagingData<BufferedImage>) {
        this.image = image
    }

    private fun forward_pass(): FloatArray {
        println(modelName)
        println(inputName)
        println(outputName)
        val tfModel = TensorflowModelInterface(null, modelName, inputName, outputName, 6)

        println("TFmodel: " + tfModel)
        val image1 = image.getImageDataAsFloatArray()
        println("Input image: " + image1.size)
        return tfModel.runModel(image1, 1, dims, dims, 3)
    }

    fun classify() : List<Float> {
        val answers : FloatArray = forward_pass()
        return answers.asList()
    }

    fun diagnose() : FloatArray {
        return forward_pass()
    }


    override val attributes: AttributeCollection
        get() = image.attributes
}