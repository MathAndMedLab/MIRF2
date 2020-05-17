package com.mirf.features.dicomimage.alg

import com.mirf.core.data.AttributeCollection
import com.mirf.core.data.Data
import com.mirf.core.data.medimage.ImagingData
import com.mirf.features.console.utils.Transp
import com.mirf.features.dicomimage.data.DicomData
import com.pixelmed.dicom.TagFromName
import java.awt.image.BufferedImage

class IHDImageData : Data {
    lateinit var imgData: ImagingData<BufferedImage>
        private set
    var cTasFloatArray: FloatArray
        private set
    var slope : Int = 1
        private set
    var intercept : Int = 0
        private set

    constructor(imgData: ImagingData<BufferedImage>, slope : Int, intercept: Int) {
        this.imgData = imgData
        this.slope = slope
        this.intercept = intercept
        cTasFloatArray = FloatArray(imgData.getImageDataAsShortArray().size)
        for (i in imgData.getImageDataAsShortArray().indices) {
            cTasFloatArray[i] = imgData.getImageDataAsShortArray()[i].toFloat()
        }
    }

    private constructor(imgData: ImagingData<BufferedImage>, pixels: FloatArray) {
        this.imgData = imgData
        cTasFloatArray = FloatArray(pixels.size)
        for (i in cTasFloatArray.indices) {
            cTasFloatArray[i] = pixels[i]
        }
    }

    private fun normalizer(arr: FloatArray, mini: Float, maxi: Float): FloatArray {
        for (i in arr.indices) {
            arr[i] = (arr[i] - mini) / (maxi - mini)
        }
        return arr
    }

    private fun windowing(window_center: Int, window_width: Int): IHDImageData {
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
        return IHDImageData(imgData, result)
    }

    fun concatCTImagesAndGetCTImage(images: Array<IHDImageData>): IHDImageData {
        val mix = Transp.flatten(Transp.transp_image(images[0].cTasFloatArray, images[1].cTasFloatArray, images[2].cTasFloatArray))
        return IHDImageData(imgData, mix)
    }

    fun brain_window(): IHDImageData {
        return windowing(40, 80)
    }

    fun subdural_window(): IHDImageData {
        return windowing(80, 200)
    }

    fun bone_window(): IHDImageData {
        return windowing(600, 2000)
    }

    fun stroke_window(): IHDImageData {
        return windowing(40, 40)
    }

    fun temporal_window(): IHDImageData {
        return windowing(600, 2800)
    }

    fun soft_window(): IHDImageData {
        return windowing(40, 380)
    }

    fun lungs_window(): IHDImageData {
        return windowing(600, 1500)
    }

    fun mediastinum_window(): IHDImageData {
        return windowing(50, 350)
    }

    fun soft_tissues_window(): IHDImageData {
        return windowing(50, 400)
    }

    fun liver_window(): IHDImageData {
        return windowing(30, 150)
    }

    fun spine_soft_tissues_window(): IHDImageData {
        return windowing(50, 400)
    }

    fun spine_bone_window(): IHDImageData {
        return windowing(400, 1800)
    }

    override val attributes: AttributeCollection
        get() = imgData.attributes
}