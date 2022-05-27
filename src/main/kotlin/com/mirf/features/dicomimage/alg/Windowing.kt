package com.mirf.features.dicomimage.alg

class Windowing(slope: Int, intercept: Int) {
    var slope: Int = slope
        private set
    var intercept: Int = intercept
        private set

    private fun windowing(cTasFloatArray: FloatArray, window_center: Int, window_width: Int): FloatArray {
        val windowMin = window_center - window_width / 2
        val windowMax = window_center + window_width / 2
        val pixels = FloatArray(cTasFloatArray.size)
        var maxi = -999999f
        var mini = 999999f
        for (i in cTasFloatArray.indices) {
            pixels[i] = cTasFloatArray[i]
            pixels[i] = pixels[i] * slope + intercept
            if (pixels[i] < windowMin) pixels[i] = windowMin.toFloat() else if (pixels[i] > windowMax) pixels[i] =
                windowMax.toFloat()
            if (pixels[i] > maxi) maxi = pixels[i]
            if (pixels[i] < mini) mini = pixels[i]
        }
        return normalizer(pixels, mini, maxi)
    }

    private fun normalizer(arr: FloatArray, mini: Float, maxi: Float): FloatArray {
        for (i in arr.indices) {
            arr[i] = (arr[i] - mini) / (maxi - mini)
        }
        return arr
    }

    fun subduralWindow(cTasFloatArray: FloatArray): FloatArray {
        return windowing(cTasFloatArray, 80, 200)
    }

    fun softWindow(cTasFloatArray: FloatArray): FloatArray {
        return windowing(cTasFloatArray, 600, 2000)
    }

    fun strokeWindow(cTasFloatArray: FloatArray): FloatArray {
        return windowing(cTasFloatArray, 40, 40)
    }

    fun temporalWindow(cTasFloatArray: FloatArray): FloatArray {
        return windowing(cTasFloatArray, 600, 2800)
    }

    fun boneWindow(cTasFloatArray: FloatArray): FloatArray {
        return windowing(cTasFloatArray, 40, 380)
    }

    fun brainWindow(cTasFloatArray: FloatArray): FloatArray {
        return windowing(cTasFloatArray, 40, 80)
    }

    fun lungsWindow(cTasFloatArray: FloatArray): FloatArray {
        return windowing(cTasFloatArray, 600, 1500)
    }

    fun mediastinumWindow(cTasFloatArray: FloatArray): FloatArray {
        return windowing(cTasFloatArray, 50, 350)
    }

    fun softTissuesWindow(cTasFloatArray: FloatArray): FloatArray {
        return windowing(cTasFloatArray, 50, 400)
    }

    fun liverWindow(cTasFloatArray: FloatArray): FloatArray {
        return windowing(cTasFloatArray, 30, 150)
    }

    fun spineSoftTissuesWindow(cTasFloatArray: FloatArray): FloatArray {
        return windowing(cTasFloatArray, 50, 400)
    }

    fun spineBoneWindow(cTasFloatArray: FloatArray): FloatArray {
        return windowing(cTasFloatArray, 400, 1800)
    }
}