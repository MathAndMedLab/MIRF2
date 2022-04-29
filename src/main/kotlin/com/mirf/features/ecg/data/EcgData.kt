package com.mirf.features.ecg.data

import com.mirf.core.data.AttributeCollection
import com.mirf.core.data.MirfData
import java.io.Serializable

class EcgData(override val attributes: AttributeCollection) : MirfData(), Serializable {

    fun getAnalogSignal(type: EcgLeadType): DoubleArray {
        val lead = this.attributes[EcgAttributes.LEADS][type]
        return if (lead != null) {
            if (this.attributes[EcgAttributes.RAW_FORMAT] == 212)
                lead.map { ((it - 1024) / this.attributes[EcgAttributes.ADC_GAIN][type]!!).toDouble() }.toDoubleArray()
            else
                lead.map { (it / this.attributes[EcgAttributes.ADC_GAIN][type]!!).toDouble() }.toDoubleArray()
        } else
            throw EcgFormatException("Lead {} is not stored".format(type.toString()))
    }
}