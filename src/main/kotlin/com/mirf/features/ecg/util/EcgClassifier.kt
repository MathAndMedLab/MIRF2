package com.mirf.features.ecg.util

import com.mirf.core.data.medimage.BufferedImageSeries
import com.mirf.features.deeplearning.tensorflow.TensorflowModelInterface
import com.mirf.features.ecg.data.EcgArrhythmiaType

object EcgClassifier {

    val mirfToTfNotations = hashMapOf<EcgArrhythmiaType, Int>(
            EcgArrhythmiaType.APC to 0, EcgArrhythmiaType.LBB to 1,
            EcgArrhythmiaType.NOR to 2, EcgArrhythmiaType.PAB to 3,
            EcgArrhythmiaType.PVC to 4, EcgArrhythmiaType.RBB to 5,
            EcgArrhythmiaType.VEB to 6, EcgArrhythmiaType.VFW to 7
    )

    fun classify(series: BufferedImageSeries) : EcgDiagnosis {


        val modelName = "src/main/resources/ecg/ecg_classifier.pb"
        val tfModel = TensorflowModelInterface(null, modelName, "conv2d_1_input", "dense_2/Softmax", 8)

        val classes = IntArray(8)
        for (img in series.images) {
            val imgArr = img.grayscaleAsFloatArray()

            var res = tfModel.runModel(imgArr, 1, 128, 128, 1)

            classes[res.indices.maxBy { res[it] } ?: -1] += 1
        }

        val arrhythmiaToNumber = HashMap<EcgArrhythmiaType, Int>()

        for (type in mirfToTfNotations) {
            arrhythmiaToNumber.put(type.key, classes[type.value])
            //println(type.key.toString() + " " + classes[type.value].toString())
        }

        return EcgDiagnosis(arrhythmiaToNumber)
    }
}