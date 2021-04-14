package com.mirf.features.dicomimage.data

import com.pixelmed.dicom.TagFromName
import java.io.Serializable

class IHDData(private var dicomAttributeCollection: DicomAttributeCollection) : DicomData(dicomAttributeCollection), Serializable {

    override fun getImageDataAsIntArray(): IntArray {
        val slope : Int = Integer.parseInt(dicomAttributeCollection.getAttributeValue(TagFromName.RescaleSlope))
        val intercept : Int = Integer.parseInt(dicomAttributeCollection.getAttributeValue(TagFromName.RescaleIntercept))
        val rezult : IntArray = IntArray(2)
        rezult[0] = slope
        rezult[1] = intercept
        return rezult
    }
}