package com.mirf.features.ecg

import com.mirf.core.data.AttributeCollection
import com.mirf.core.data.MirfData
import com.mirf.core.data.attribute.AttributeTagType
import com.mirf.core.data.attribute.DataAttributeMockup
import java.io.Serializable
import java.util.*

class EcgData(override val attributes: AttributeCollection) : MirfData(), Serializable {

    fun getAnalogSignal(type: EcgLeadType): DoubleArray {
        val lead = this.attributes.get(EcgAttributes.LEADS).get(type)
        if (lead != null) {
            if (this.attributes.get(EcgAttributes.RAW_FORMAT) == 212)
                return lead.map{((it - 1024) / this.attributes.get(EcgAttributes.ADC_GAIN).get(type)!!).toDouble()}.toDoubleArray()
            else
                return lead.map{(it / this.attributes.get(EcgAttributes.ADC_GAIN).get(type)!!).toDouble()}.toDoubleArray()
        }
        else
            throw EcgFormatException("Lead {} is not stored".format(type.toString()))
    }
}