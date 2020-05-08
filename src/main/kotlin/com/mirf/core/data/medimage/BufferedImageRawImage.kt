package com.mirf.core.data.medimage

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


class BufferedImageRawImage : RawImageData {
    private val _image: BufferedImage

    constructor(image : BufferedImage): super() {
        this._image = image
    }

    override fun getHeight(): Int {
        return _image.getHeight()
    }

    override fun getWidth(): Int {
        return _image.getWidth()
    }

    override fun getImage(): ByteArray? {
        val baos = ByteArrayOutputStream()
        ImageIO.write(this._image, "jpg", baos);
        return baos.toByteArray()
    }

    fun grayscaleAsFloatArray(): FloatArray {
        val res = FloatArray(getWidth() * getHeight())

        var gray : Float = 0.0F
        for (i in 0..getWidth() - 1) {
            var flag = false;
            for (j in 0..this._image.height - 1) {
                var pixel = this._image.getRGB(i, j) and 0xffffff
                gray = ((pixel and 0x0000ff) + ((pixel and 0x00ff00) shr 8) + ((pixel and 0xff0000) shr 16)) / 3.toFloat()
                res[i * getWidth() + j] = gray / 255.toFloat()
//                if (!flag) {
//
//                    if (((pixel and 0x0000ff) + (pixel and 0x00ff00) shr 8 + (pixel and 0xff0000) shr 16) != 255 * 3)  {
//                        flag = true
//                        println(i.toString() + " " + j.toString())
//                        println((pixel and 0x0000ff) + ((pixel and 0x00ff00) shr 8) + ((pixel and 0xff0000) shr 16))
//                        println(pixel and 0x0000ff)
//                        println((pixel and 0x00ff00) shr 8)
//                        println((pixel and 0xff0000) shr 16)
//                    }
//                }
            }
        }
        return res
    }
}