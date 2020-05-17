package com.mirf.playground.IHD

import com.mirf.core.data.AttributeCollection
import com.mirf.core.data.Data
import com.mirf.features.deeplearning.tensorflow.TensorflowModelInterface
import com.mirf.features.dicomimage.alg.IHDImageData

class IntracranialHemorrhageDetectionDiagnosis : Data {
    // IntracranialHemorrhageDetection
    lateinit var image : IHDImageData
        private set
    var modelName  = "/Users/alexander.savelyev/IdeaProjects/Medical-images-research-framework/src/test/resources/dicomDataTest/512_512_3.pb"
        private set
    var inputName = "input_2"
        private set
    var outputName = "dense_output_1/Sigmoid"
        private set
    var dims : Long = 512
        private set

    constructor(image: IHDImageData) {
        this.image = image
    }

    private fun forward_pass(): FloatArray {
        val tfModel = TensorflowModelInterface(null, modelName, inputName, outputName, 6)

        return tfModel.runModel(image.cTasFloatArray, 1, dims, dims, 3)
    }

    fun classify() {
        val answers : FloatArray = forward_pass()
        println(answers.asList())
    }

    fun diagnose() : FloatArray {
        return forward_pass()
    }


    override val attributes: AttributeCollection
        get() = image.attributes
}