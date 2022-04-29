package com.mirf.features.ecg.util

import com.mirf.core.data.AttributeCollection
import com.mirf.core.data.attribute.DataAttributeCreator
import com.mirf.features.ecg.data.EcgAttributes
import com.mirf.features.ecg.data.EcgData
import com.mirf.features.ecg.data.EcgLeadType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors
import kotlin.math.min


object EcgCleaner {
    fun filterNoises(ecgData: EcgData, leadType: EcgLeadType): EcgData {
        val sb = StringBuilder()
        val lead = ecgData.getAnalogSignal(leadType)
        for (x in lead.copyOfRange(0, min(3000, lead.size))) {
            sb.append(x)
            sb.append(" ")
        }

        val filteredSignal = executeFilteringScriptAndGetFilteredSignal(sb.toString())
        val attributes = AttributeCollection()
        val filteredEcg = hashMapOf(EcgLeadType.II to filteredSignal)
        attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.LEADS_FILTERED, filteredEcg))
        return EcgData(attributes)
    }

    private fun executeFilteringScriptAndGetFilteredSignal(ecgArrayString: String): DoubleArray {
        val pb = ProcessBuilder("python3",
            "/home/alexandra/MIRF2/src/main/resources/filteringpy/ecgFiltering.py",
            ecgArrayString)
        val p = pb.start()
        p.waitFor()
        val bfr = BufferedReader(InputStreamReader(p.inputStream))
        var line: String?
        var filteredValues = DoubleArray(0)
        while (bfr.readLine().also { line = it } != null) {
            val lineValues: List<Double> = line?.split(' ')?.stream()?.filter { it.isNotEmpty() }?.map { it.toDouble() }
                ?.collect(Collectors.toList()) as List<Double>
            filteredValues = filteredValues.plus(lineValues)
        }

        return filteredValues
    }
}

