package com.mirf.features.ecg.util

import com.mirf.core.data.medimage.BufferedImageSeries
import com.mirf.features.deeplearning.tensorflow.TensorflowModelInterface

object EcgClassifier {

    fun classify(series: BufferedImageSeries) : EcgDiagnosis {

        val modelName = "src/main/resources/ecg/ecg_classifier.pb"
        val tfModel = TensorflowModelInterface(null, modelName, "conv2d_1_input", "dense_2/Softmax", 8)

        val classes = IntArray(8)
        for (img in series.images) {
            val imgArr = img.grayscaleAsFloatArray()

            var res = tfModel.runModel(imgArr, 1, 128, 128, 1)

            classes[res.indices.maxBy { res[it] } ?: -1] += 1
        }

        return EcgDiagnosis(classes.indices.maxBy{ classes[it]} ?: -1)
    }
}