package com.mirf.features.dicomimage.alg

class Windowing(slope: Int, intercept: Int) {
    var slope : Int = slope
        private set
    var intercept : Int = intercept
        private set

    private fun windowing(cTasFloatArray : FloatArray, window_center: Int, window_width: Int): FloatArray {
        val window_min = window_center - window_width / 2
        val window_max = window_center + window_width / 2
        val pixels = FloatArray(cTasFloatArray.size)
        var maxi = -999999f
        var mini = 999999f
        for (i in cTasFloatArray.indices) {
            pixels[i] = cTasFloatArray[i]
            pixels[i] = pixels[i] * slope + intercept
            if (pixels[i] < window_min) pixels[i] = window_min.toFloat() else if (pixels[i] > window_max) pixels[i] = window_max.toFloat()
            if (pixels[i] > maxi) maxi = pixels[i]
            if (pixels[i] < mini) mini = pixels[i]
        }
        val result = normalizer(pixels, mini, maxi)
        return result
    }

    private fun normalizer(arr: FloatArray, mini: Float, maxi: Float): FloatArray {
        for (i in arr.indices) {
            arr[i] = (arr[i] - mini) / (maxi - mini)
        }
        return arr
    }

    fun subdural_window(cTasFloatArray : FloatArray): FloatArray {
        return windowing(cTasFloatArray,80, 200)
    }

    fun soft_window(cTasFloatArray : FloatArray): FloatArray {
        return windowing(cTasFloatArray,600, 2000)
    }

    fun stroke_window(cTasFloatArray : FloatArray): FloatArray {
        return windowing(cTasFloatArray,40, 40)
    }

    fun temporal_window(cTasFloatArray : FloatArray): FloatArray {
        return windowing(cTasFloatArray,600, 2800)
    }

    fun bone_window(cTasFloatArray : FloatArray): FloatArray {
        return windowing(cTasFloatArray,40, 380)
    }

    fun brain_window(cTasFloatArray : FloatArray): FloatArray {
        return windowing(cTasFloatArray, 40, 80)
    }

    fun lungs_window(cTasFloatArray : FloatArray): FloatArray {
        return windowing(cTasFloatArray,600, 1500)
    }

    fun mediastinum_window(cTasFloatArray : FloatArray): FloatArray {
        return windowing(cTasFloatArray,50, 350)
    }

    fun soft_tissues_window(cTasFloatArray : FloatArray): FloatArray {
        return windowing(cTasFloatArray,50, 400)
    }

    fun liver_window(cTasFloatArray : FloatArray): FloatArray {
        return windowing(cTasFloatArray,30, 150)
    }

    fun spine_soft_tissues_window(cTasFloatArray : FloatArray): FloatArray {
        return windowing(cTasFloatArray,50, 400)
    }

    fun spine_bone_window(cTasFloatArray : FloatArray): FloatArray {
        return windowing(cTasFloatArray,400, 1800)
    }
}