package com.mirf.features.ecg

import com.mirf.core.data.attribute.AttributeTagType
import com.mirf.core.data.attribute.DataAttributeMockup

object EcgAttributes {

    val LEADS_PRESENTED = DataAttributeMockup<ArrayList<EcgLeadType>>("leadsPresented", "1",
            AttributeTagType.ECG)
    val LEADS = DataAttributeMockup<HashMap<EcgLeadType, ArrayList<Short>>>("leads", "2",
            AttributeTagType.ECG)
    val NUMBER_OF_SAMPLES = DataAttributeMockup<Int>("numSamples", "3",
            AttributeTagType.ECG)
    val SAMPLING_FREQUENCY = DataAttributeMockup<Int>("samplingFrequency", "4",
            AttributeTagType.ECG)
    val FILE_ID = DataAttributeMockup<String>("fileId", "5",
            AttributeTagType.ECG)
    val RAW_FORMAT = DataAttributeMockup<HashMap<EcgLeadType,Int>>("rawFormat", "6",
            AttributeTagType.ECG)
    val ADC_GAIN = DataAttributeMockup<HashMap<EcgLeadType, Float>>("adcGain", "7",
            AttributeTagType.ECG)
    /**
     * ADC units per millivolt
     */
    val ADC_RESOLUTION = DataAttributeMockup<HashMap<EcgLeadType, Int>>("adcResolution", "8",
            AttributeTagType.ECG)
    val ADC_ZERO_VALUE = DataAttributeMockup<HashMap<EcgLeadType, Short>>("zeroValueADC", "9",
            AttributeTagType.ECG)
    val CHECKSUMS = DataAttributeMockup<HashMap<EcgLeadType, Short>>("checksums", "10",
            AttributeTagType.ECG)
    val INITIAL_VALUES = DataAttributeMockup<HashMap<EcgLeadType, Short>>("initialvalues", "11",
            AttributeTagType.ECG)
}