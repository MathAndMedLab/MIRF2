package com.mirf.features.dicomimage.data

import com.mirf.playground.IHD.IntracranialHemorrhageDetectionWorkflow
import com.pixelmed.dicom.TagFromName

class IHDData(private var dicomAttributeCollection: DicomAttributeCollection) : DicomData(dicomAttributeCollection) {

    override fun getImageDataAsIntArray(): IntArray {
        val slope : Int = Integer.parseInt(dicomAttributeCollection.getAttributeValue(TagFromName.RescaleSlope))
        val intercept : Int = Integer.parseInt(dicomAttributeCollection.getAttributeValue(TagFromName.RescaleIntercept))
        val rezult : IntArray = IntArray(2)
        rezult[0] = slope
        rezult[1] = intercept
        return rezult
    }
}