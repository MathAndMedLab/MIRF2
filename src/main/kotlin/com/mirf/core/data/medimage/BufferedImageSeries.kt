package com.mirf.core.data.medimage

import com.mirf.core.data.AttributeCollection
import com.mirf.core.data.MirfData
import java.io.Serializable

class BufferedImageSeries(val images: List<BufferedImageRawImage>,
                          attributes: AttributeCollection = AttributeCollection()): MirfData(), Serializable {

}