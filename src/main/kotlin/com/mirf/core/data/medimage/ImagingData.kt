package com.mirf.core.data.medimage

import com.mirf.core.array.BooleanArray2D
import com.mirf.core.data.Data

interface ImagingData<I> : Data{

    fun getImage(): I
    fun getImageDataAsShortArray(): ShortArray
    fun getImageDataAsByteArray(): ByteArray
    fun getImageDataAsIntArray(): IntArray
    fun getImageDataAsFloatArray(): FloatArray

    fun copy(): ImagingData<I>
    fun applyMask(mask: BooleanArray2D)

    val width: Int
    val height: Int
}