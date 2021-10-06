package com.mirf.features.dicomimage.data

import com.mirf.core.array.BooleanArray2D
import com.mirf.core.data.AttributeCollection

import com.mirf.core.data.medimage.ImagingData
import com.pixelmed.dicom.AttributeTag
import com.pixelmed.dicom.DicomOutputStream
import com.pixelmed.dicom.TagFromName
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.stream.FileImageInputStream
import kotlin.math.sqrt

/**
 * Realize interface ImageData on dicom image format
 */
open class DicomData() : ImagingData<BufferedImage> {
    private var bitsAllocated: Int = 0
    private lateinit var byteArray: ByteArray
    private lateinit var shortArray: ShortArray
    private lateinit var intArray: IntArray
    private lateinit var floatArray: FloatArray
    private lateinit var cleanByteArrayPixelData: ByteArray
    private lateinit var dicomAttributeCollection: DicomAttributeCollection

    constructor(dicomAttributeCollection: DicomAttributeCollection) : this() {
        this.dicomAttributeCollection = dicomAttributeCollection
        bitsAllocated = Integer.parseInt(dicomAttributeCollection.getAttributeValue(TagFromName.BitsAllocated))
        analisePixelData()
    }

    constructor(array: FloatArray) : this() {
        //this.dicomAttributeCollection = dicomAttributeCollection
        floatArray = FloatArray(array.size)
        byteArray = ByteArray(array.size)
        intArray = IntArray(array.size)
        shortArray = ShortArray(array.size)

        for (i in array.indices) {
            floatArray[i] = array[i]
            byteArray[i] = array[i].toByte()
            intArray[i] = array[i].toInt()
            shortArray[i] = array[i].toShort()
        }
    }

    fun getAtrib() : DicomAttributeCollection {
        return dicomAttributeCollection
    }

    /**
     * Analise image and initialized byteArray, shortArray and IntArray
     */
    private fun analisePixelData() {
        val tempFile = File.createTempFile("pixeldata-", ".txt")
        pixelDataWriteToFile(tempFile.name)
        val dirtyByteArrayPixelData: ByteArray = readPixelDataFileAndWriteToDirtyByteArray(tempFile.name)
        convertDirtyByteArrayToCleanByteArray(dirtyByteArrayPixelData)
        println("Deleted temp pixel file: " + tempFile.delete())

        when (bitsAllocated) {
            8 -> {
                byteArray = cleanByteArrayPixelData
                shortArray = byteArrayToShortArray(byteArray)
                intArray = byteArrayToIntArray(byteArray)
            }
            16 -> {
                shortArray = cleanByteArrayPixelDataToShortArray(cleanByteArrayPixelData)
                byteArray = shortArrayToByteArray(shortArray)
                intArray = shortArrayToIntArray(shortArray)
            }
            32 -> intArray = cleanByteArrayPixelDataToIntArray(cleanByteArrayPixelData)
        }
    }

    /**
     * Convert image data from file to int array
     */
    private fun cleanByteArrayPixelDataToIntArray(cleanByteArrayPixelData: ByteArray): IntArray {
        val intArray = IntArray(cleanByteArrayPixelData.size / 4)
        var j = 0
        var i = 0
        while (i < cleanByteArrayPixelData.size) {
            val temp = ByteArray(4)
            temp[0] = cleanByteArrayPixelData[i]
            temp[1] = cleanByteArrayPixelData[i + 1]
            temp[2] = cleanByteArrayPixelData[i + 2]
            temp[3] = cleanByteArrayPixelData[i + 3]
            intArray[j] = bytesToInt(temp)
            j++
            i += 4
        }
        return intArray
    }

    /**
     * Convert byte array to int value
     */
    private fun bytesToInt(bytes: ByteArray): Int {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
    }

    /**
     * Convert short array to int array
     */
    private fun shortArrayToIntArray(shortArray: ShortArray): IntArray {
        val intArray = IntArray(shortArray.size)
        for (i in intArray.indices) {
            intArray[i] = shortArray[i].toInt()
        }
        return intArray
    }
    /**
     * Convert short array to byte array use "grayscale standard display function"
     */
    private fun shortArrayToByteArray(shortArray: ShortArray): ByteArray {
        val byteArray = ByteArray(shortArray.size)
        try {
            val m: Double = 255.0 / Integer.parseInt(dicomAttributeCollection.getAttributeValue(TagFromName.WindowWidth))
            val x1: Int = Integer.parseInt(dicomAttributeCollection.getAttributeValue(TagFromName.WindowCenter)) -
                    Integer.parseInt(dicomAttributeCollection.getAttributeValue(TagFromName.WindowCenter)) / 2
            val b: Int = (-(m * x1)).toInt()

            val lut: HashMap<Short, Byte> = HashMap()
            val min: Int = Integer.parseInt(dicomAttributeCollection.getAttributeValue(TagFromName.SmallestImagePixelValue))
            val max: Int = Integer.parseInt(dicomAttributeCollection.getAttributeValue(TagFromName.LargestImagePixelValue))
            var j: Int = min
            while (j <= max) {
                var temp = ((m * j) + b).toInt()
                if (temp > 127) temp = 127
                else if (temp < -128) temp = -128
                lut[j.toShort()] = temp.toByte()
                j++
            }
            val byteArray = ByteArray(shortArray.size)
            for (i in shortArray.indices) {
                byteArray[i] = lut[shortArray[i]] as Byte
            }
        } catch (e: Exception) {
            //e.printStackTrace()
        }
        return byteArray
    }

    /**
     * Convert image data from file to int array
     */
    private fun cleanByteArrayPixelDataToShortArray(cleanByteArray : ByteArray): ShortArray {
        val shortArray = ShortArray(cleanByteArray.size / 2)
        var j = 0
        var i = 0
        while (i < cleanByteArray.size) {
            val temp = ByteArray(2)
            temp[0] = cleanByteArray[i]
            temp[1] = cleanByteArray[i + 1]
            shortArray[j] = bytesToShort(temp)
            j++
            i += 2
        }
        return shortArray
    }

    /**
     * Convert byte array to short value
     */
    private fun bytesToShort(bytes: ByteArray): Short {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short
    }

    /**
     * Convert byte array to int array
     */
    private fun byteArrayToIntArray(byteArray: ByteArray) : IntArray {
        val intArray = IntArray(byteArray.size)
        for (i in intArray.indices) {
            intArray[i] = byteArray[i].toInt()
        }
        return intArray
    }

    /**
     * Convert byte array to short array
     */
    private fun byteArrayToShortArray(byteArray: ByteArray) : ShortArray {
        val shortArray = ShortArray(byteArray.size)
        for (i in shortArray.indices) {
            shortArray[i] = byteArray[i].toShort()
        }
        return shortArray
    }

    /**
     * Convert byte array image with preamble to image byte array image
     */
    private fun convertDirtyByteArrayToCleanByteArray(dirtyByteArrayPixelData: ByteArray) {
        cleanByteArrayPixelData = ByteArray(dirtyByteArrayPixelData.size - 144)
        for (i in cleanByteArrayPixelData.indices) {
            cleanByteArrayPixelData[i] = dirtyByteArrayPixelData[i + 144]
        }
    }

    /**
     * Read image and write to byte array image with preamble
     */
    private fun readPixelDataFileAndWriteToDirtyByteArray(tempPixelDataFileName: String): ByteArray {
        val file = File(tempPixelDataFileName)
        val imageInputStream = FileImageInputStream(file)
        val dirtyByteArrayPixelData = ByteArray(file.length().toInt())
        imageInputStream.read(dirtyByteArrayPixelData)
        imageInputStream.close()
        return dirtyByteArrayPixelData
    }

    /**
     * PixelData write to support file
     */
    private fun pixelDataWriteToFile(tempPixelDataFileName: String) {
        val pixelData = dicomAttributeCollection.getAttributePixelData()
        val outputStream = FileOutputStream(File(tempPixelDataFileName))
        val transferSyntaxUID = dicomAttributeCollection.getAttributeValue(TagFromName.TransactionUID)
        val dicomOutputStream = DicomOutputStream(outputStream, "", transferSyntaxUID)
        pixelData.write(dicomOutputStream)
    }

    /**
     * Get dicom image in the view BufferedImage
     */
    override fun getImage(): BufferedImage {
        var no_window = FloatArray(shortArray.size)
        var maxi = -999999f
        var mini = 999999f

        for (i in shortArray.indices) {
            no_window[i] = shortArray.get(i).toFloat()
            if (no_window[i] > maxi) maxi = no_window[i]
            if (no_window[i] < mini) mini = no_window[i]
        }
        val rezult = normalizer(no_window, mini, maxi)
        return toGrayscale(rezult, sqrt(shortArray.size.toFloat()).toInt(), sqrt(shortArray.size.toFloat()).toInt())
    }

    private fun toRGB(value: Float): Int {
        val part = Math.round(value * 255)
        return part * 0x10101
    }

    private fun normalizer(data: FloatArray, mini: Float, maxi: Float): FloatArray {
        for (i in data.indices) {
            data[i] = (data[i] - mini) / (maxi - mini)
        }
        return data
    }

    private fun toGrayscale(pixels: FloatArray, height: Int, width: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        var k = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                image.setRGB(x, y, toRGB(pixels[k]))
                k++
            }
        }
        return image
    }

    /**
     * Get dicom image in the view ShortArray
     */
    override fun getImageDataAsShortArray(): ShortArray {
        if(bitsAllocated == 32)
            throw DicomDataException("Can't get shortArray because bits allocated = 32")
        return shortArray
    }

    fun getAttributeValue(attrTag: AttributeTag): String {
        return dicomAttributeCollection.getAttributeValue(attrTag)
    }

    /**
     * Get dicom image in the view ByteArray
     */
    override fun getImageDataAsByteArray(): ByteArray {
        if(bitsAllocated == 32)
            throw DicomDataException("Can't get byteArray because bits allocated = 32")
        return byteArray
    }

    /**
     * Get dicom image in the view IntArray
     */
    override fun getImageDataAsIntArray(): IntArray {
        return intArray
    }

    override fun copy(): ImagingData<BufferedImage> {
        return DicomData(dicomAttributeCollection.clone())
    }

    override fun applyMask(mask: BooleanArray2D) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val width: Int
        get() = dicomAttributeCollection.buildHumanReadableImage().width
    override val height: Int
        get() = dicomAttributeCollection.buildHumanReadableImage().height
    override val attributes: AttributeCollection
        get() = dicomAttributeCollection

    override fun getImageDataAsFloatArray(): FloatArray {
        val floatArray = FloatArray(shortArray.size)
        for (i in floatArray.indices) {
            floatArray[i] = shortArray[i].toFloat()
        }
        return floatArray
    }
}