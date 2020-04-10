package com.mirf.core.data.medimage

import com.mirf.core.data.AttributeCollection
import com.mirf.core.data.MirfData

class BufferedImageSeries(val images: List<BufferedImageRawImage>,
                          attributes: AttributeCollection = AttributeCollection()): MirfData() {

}