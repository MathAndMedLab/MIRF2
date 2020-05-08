package com.mirf.features.ecg.util

import com.mirf.core.data.AttributeCollection
import com.mirf.core.data.attribute.DataAttributeCreator
import com.mirf.features.ecg.EcgAttributes
import com.mirf.features.ecg.EcgData
import com.mirf.features.ecg.EcgLeadType
import kotlin.math.min

//import com.mathworks.engine.MatlabEngine


object EcgCleaner {
    fun filterNoises(ecgData: EcgData, leadType: EcgLeadType) : EcgData {

//        val eng = MatlabEngine.startMatlab()
//        eng.evalAsync("[X, Y] = meshgrid(-2:0.2:2);")
//        eng.evalAsync("Z = X .* exp(-X.^2 - Y.^2);")
//        //val Z = eng.getVariable<Array<Any>>("Z")
//        eng.close()

        val lead = ecgData.getAnalogSignal(leadType)

        val attributes = AttributeCollection()

        val filteredEcg = hashMapOf<EcgLeadType, DoubleArray>(EcgLeadType.II to lead.copyOfRange(0, min(5000, lead.size)))

        attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.LEADS_FILTERED, filteredEcg))

        return EcgData(attributes)
    }


//    without_white = wden(lead, 'sqtwolog', 's', 'one', 3, 'sym12');
//    wander = wden(lead, 'heursure', 's', 'one', 8, 'sym8');
//    denoised = lead1_mv - wander;

}

