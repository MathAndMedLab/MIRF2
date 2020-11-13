package com.mirf.core.data.medimage

import java.awt.image.BufferedImage
import java.io.*
import javax.imageio.ImageIO


class BufferedImageRawImage : RawImageData, Serializable {
    @Transient
    private var _image: BufferedImage

    constructor(image : BufferedImage): super() {
        this._image = image
    }

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.defaultWriteObject()
        val buffer = ByteArrayOutputStream()
        ImageIO.write(_image, "jpg", buffer)
        out.writeInt(buffer.size()); // Prepend image with byte count
        buffer.writeTo(out);         // Write image

//        out.defaultWriteObject()
//        out.writeInt(1) // how many images are serialized?
//        ImageIO.write(_image, "png", out) // png is lossless

//        for (eachImage in images) {
//            ImageIO.write(eachImage, "png", out) // png is lossless
//        }
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        val size = `in`.readInt() // Read byte count
        val buffer = ByteArray(size)
        `in`.readFully(buffer) // Make sure you read all bytes of the image

        _image = ImageIO.read(ByteArrayInputStream(buffer))

//        `in`.defaultReadObject()
////        val imageCount = `in`.readInt()
//        _image = ImageIO.read(`in`)
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