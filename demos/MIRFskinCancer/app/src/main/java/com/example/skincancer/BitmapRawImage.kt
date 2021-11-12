package com.example.skincancer

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import android.R.array
import android.opengl.ETC1.getHeight
import java.nio.ByteBuffer
import android.graphics.Color
import core.data.medimage.RawImageData
import core.data.Data
import core.data.AttributeCollection

/**
 * BitmapRawImage is used to represent Bitmap
 */
// TODO: make here medImage and clean up the code
class BitmapRawImage(image: Bitmap?) : RawImageData(), Data {
    override val attributes: AttributeCollection = AttributeCollection()
    private var _image: Bitmap? = image

    override fun getHeight(): Int {
        return _image!!.getHeight()
    }

    override fun getWidth(): Int {
        return _image!!.getWidth()
    }

    override fun getImage(): ByteArray? {
        val size = getWidth() * getHeight()
        val buffer = ByteBuffer.allocate(size)
        _image!!.copyPixelsToBuffer(buffer)
        return buffer.array()
    }

    /**
     * getFloatImageArray is used to present image as FloatArray
     *
     * @param resizedHeight - new height
     * @param resizedWidth - new width
     *
     * @return FloatArray of pixels
     */
    fun getFloatImageArray(resizedHeight: Int, resizedWidth: Int): FloatArray {
        val resized = Bitmap.createScaledBitmap(this._image!!, resizedWidth, resizedHeight, true)
        val channel = 3
        val bitmapSize = resizedWidth * resizedHeight
        var rIndex = 0
        var gIndex = 0
        var bIndex = 0
        var buff = FloatArray(channel * bitmapSize)
        var bitBuff = IntArray(bitmapSize)
        var k = 0;
        resized!!.getPixels(bitBuff, 0, resizedWidth, 0, 0, resizedWidth, resizedHeight);
        for (i in 0..resizedHeight - 1) {
            for (j in 0..resizedWidth - 1) {
                if (k >= bitmapSize - 1) {
                    break
                }
                rIndex = k * 3
                gIndex = rIndex + 1
                bIndex = gIndex + 2

                var color = bitBuff[k]
                buff[rIndex] = (Color.red(color)).toFloat()
                buff[gIndex] = (Color.green(color)).toFloat()
                buff[bIndex] = (Color.blue(color)).toFloat()
                k++
            }
        }

        return buff;
    }
}