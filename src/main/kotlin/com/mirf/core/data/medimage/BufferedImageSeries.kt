package com.mirf.core.data.medimage

import com.mirf.core.data.MirfData
import java.io.Serializable

class BufferedImageSeries(
    val images: List<BufferedImageRawImage>,
) : MirfData(), Serializable