package com.mirf.features.ecg.util

import com.mirf.core.data.AttributeCollection
import com.mirf.core.data.attribute.DataAttributeCreator
import com.mirf.features.ecg.EcgAttributes
import com.mirf.features.ecg.EcgData
import com.mirf.features.ecg.EcgLeadType
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.min


object EcgCleaner {
    fun filterNoises(ecgData: EcgData, leadType: EcgLeadType) : EcgData {

        val lead = ecgData.getAnalogSignal(leadType)

        val attributes = AttributeCollection()
        val filteredEcg = hashMapOf<EcgLeadType, DoubleArray>(EcgLeadType.II to lead.copyOfRange(0, min(5000, lead.size)))
        attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.LEADS_FILTERED, filteredEcg))
        return EcgData(attributes)
    }

    private fun File.execute(vararg arguments: String): String {
        val process = ProcessBuilder(*arguments)
                .directory(this)
                .start()
                .also { it.waitFor(2, TimeUnit.MINUTES) }

        if (process.exitValue() != 0) {
            throw Exception(process.errorStream.bufferedReader().readText())
        }
        return process.inputStream.bufferedReader().readText()
    }

    fun filterNoisesPython(ecgData: EcgData, leadType: EcgLeadType) : EcgData  {
        val lead = ecgData.getAnalogSignal(leadType)

        val python_arg = StringBuilder()
        lead.take(Math.min(5000, lead.size)).forEach{ python_arg.append(it); python_arg.append(' '); }


        var classPath = this.javaClass.getProtectionDomain().getCodeSource().getLocation().getPath()
        var filtering_pref = "../../../util/filteringpy"

        File(classPath + filtering_pref).execute(
                "python3", "ecg_filtering.py", python_arg.toString())

        val attributes = AttributeCollection()
        val filteredEcg = hashMapOf<EcgLeadType, DoubleArray>(EcgLeadType.II to lead.copyOfRange(0, min(5000, lead.size)))
        attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.LEADS_FILTERED, filteredEcg))
        return EcgData(attributes)
    }

}

