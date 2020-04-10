package com.mirf.features.ecg

import com.mirf.core.data.AttributeCollection
import com.mirf.core.data.AttributeException
import com.mirf.core.data.attribute.DataAttributeCreator
import java.io.File
import java.lang.Float.parseFloat
import java.lang.Integer.parseInt
import java.lang.Short.parseShort
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object EcgReader {

    val ptbNotationToMirfEcgLeadTypes = hashMapOf<String, EcgLeadType>(
            "i" to EcgLeadType.I, "ii" to EcgLeadType.II,"iii" to EcgLeadType.III,
            "avl" to EcgLeadType.aVL, "avr" to EcgLeadType.aVR,"avf" to EcgLeadType.aVF,
            "v1" to EcgLeadType.V1, "v2" to EcgLeadType.V2, "v3" to EcgLeadType.V3,
            "v4" to EcgLeadType.V4, "v5" to EcgLeadType.V5, "v6" to EcgLeadType.V6,
            "vx" to EcgLeadType.VX, "vy" to EcgLeadType.VY, "vz" to EcgLeadType.VZ)

    val mitBihNotationToMirfEcgLeadTypes = hashMapOf<String, EcgLeadType> (
            "MLI" to EcgLeadType.I, "MLII" to EcgLeadType.II,"MLIII" to EcgLeadType.III,
            "aVL" to EcgLeadType.aVL, "aVR" to EcgLeadType.aVR,"aVF" to EcgLeadType.aVF,
            "V1" to EcgLeadType.V1, "V2" to EcgLeadType.V2, "V3" to EcgLeadType.V3,
            "V4" to EcgLeadType.V4, "V5" to EcgLeadType.V5, "V6" to EcgLeadType.V6,
            "VX" to EcgLeadType.VX, "VY" to EcgLeadType.VY, "VZ" to EcgLeadType.VZ
    )

    fun readEcg(headerFilePath: String, dataFilePath: String, rawFormat: Int) : EcgData {
        val attributes = AttributeCollection()

        var leads : Map<EcgLeadType, ArrayList<Short>> = HashMap<EcgLeadType, ArrayList<Short>>()
        try {
            when (rawFormat) {
                16 -> {
                    readEcgHeader(headerFilePath, attributes, ptbNotationToMirfEcgLeadTypes)
                    leads = readEcgRawSignals16(dataFilePath, attributes.getAttributeValue(EcgAttributes.NUMBER_OF_SAMPLES), attributes.getAttributeValue(EcgAttributes.LEADS_PRESENTED),
                    attributes.getAttributeValue(EcgAttributes.CHECKSUMS), attributes.getAttributeValue(EcgAttributes.INITIAL_VALUES))
                }
                212 -> {
                    readEcgHeader(headerFilePath, attributes, mitBihNotationToMirfEcgLeadTypes)
                    leads = readEcgRawSignals212(dataFilePath, attributes.getAttributeValue(EcgAttributes.NUMBER_OF_SAMPLES), attributes.getAttributeValue(EcgAttributes.LEADS_PRESENTED),
                        attributes.getAttributeValue(EcgAttributes.CHECKSUMS), attributes.getAttributeValue(EcgAttributes.INITIAL_VALUES))
                }
            }
            attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.LEADS, leads))
        }
        catch (ex: AttributeException){
            throw EcgFormatException("Incompatible header file from PTB database")
        }
        return EcgData(attributes)
    }



    private fun readEcgHeader(filePath: String, attributes: AttributeCollection, notationToMirfEcgLeadTypes : HashMap<String, EcgLeadType>) {

        val info: String =   File(/*"src/main/resources/ecg/" + fileId + ".hea"*/filePath).readText()

        val infoLines = info.lines()
        //firstline: s0010_re 15 1000 38400 - fileid, num of leads, frequency, num of samples
        val firstLine = infoLines.get(0).split(" ")

        attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.FILE_ID, firstLine[0]))
        val numOfLeads = firstLine[1].toInt()
        attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.SAMPLING_FREQUENCY, firstLine[2].toInt()))
        attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.NUMBER_OF_SAMPLES, firstLine[3].toInt()))
        //fill some attributes with empty collections
        attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.LEADS_PRESENTED, LinkedHashSet<EcgLeadType>()))
        attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.ADC_GAIN, HashMap<EcgLeadType, Float>()))
        attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.ADC_RESOLUTION,  HashMap<EcgLeadType, Int>()))
        attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.ADC_ZERO_VALUE, HashMap<EcgLeadType, Short>()))
        attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.INITIAL_VALUES, HashMap<EcgLeadType, Short>()))
        attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.CHECKSUMS, HashMap<EcgLeadType, Short>()))

        //leadInfo: s0010_re.dat 16 2000 16 0 -489 -8337 0 i - nameOfFile, format, ADC gain, ADC resolution, ADC zero value, first value in each lead, checksum, some kind of rudiment, lead type
        //indices:             0  1    2  3 4    5     6 7 8
        for (i in 1..parseInt(firstLine[1])) {
            val leadType: EcgLeadType?
            val leadInfo = infoLines.get(i).split(" ")
            try {
                leadType = notationToMirfEcgLeadTypes.get(leadInfo[8])
                if (listOf(EcgLeadType.VX, EcgLeadType.VY, EcgLeadType.VZ).contains(leadType))
                    continue
            } catch(ex: Exception) {
                throw EcgFormatException("Incompatible header file")
            }
            when (leadType) {
                null -> throw EcgFormatException("Incompatible header file")
                else -> {
                    try {
                        attributes.getAttributeValue(EcgAttributes.LEADS_PRESENTED).add(leadType)
                        if (!attributes.hasAttribute("rawFormat"))
                            attributes.add(DataAttributeCreator.createFromMock(EcgAttributes.RAW_FORMAT, parseInt(leadInfo[1])))
                        attributes.getAttributeValue(EcgAttributes.ADC_GAIN).put(leadType, parseFloat(leadInfo[2]))
                        attributes.getAttributeValue(EcgAttributes.ADC_RESOLUTION).put(leadType, parseInt(leadInfo[3]))
                        attributes.getAttributeValue(EcgAttributes.ADC_ZERO_VALUE).put(leadType, parseShort(leadInfo[4]))
                        attributes.getAttributeValue(EcgAttributes.INITIAL_VALUES).put(leadType, parseShort(leadInfo[5]))
                        attributes.getAttributeValue(EcgAttributes.CHECKSUMS).put(leadType, parseShort(leadInfo[6]))
                    } catch (ex: NumberFormatException) {
                        throw EcgFormatException("Incompatible header file")
                    }
                }
            }
        }
    }

    private fun readEcgRawSignals212(dataFilePath: String, numOfSamples: Int, leadsPresent: LinkedHashSet<EcgLeadType>,
                                    checksumsExpected: Map<EcgLeadType, Short>, checkFirstValues: Map<EcgLeadType, Short>): Map<EcgLeadType, ArrayList<Short>> {
        val rawSamples: ByteArray = File(/*"src/main/resources/ecg/" + fileId + ".dat"*/dataFilePath).readBytes()
        val leads = HashMap<EcgLeadType, ArrayList<Short>>()
        val checksumsReal = HashMap<EcgLeadType, Short>()

        if (rawSamples.size != leadsPresent.size * numOfSamples * 3 / 2)
            throw EcgFormatException("Ecg data is broken")

        val leadFirst = leadsPresent.elementAt(0);
        val leadSecond = leadsPresent.elementAt(1);

        val readSimultaneousSignals = { startOfLine: Int, rawSamples: ByteArray,
                                        checksumsReal: HashMap<EcgLeadType, Short>, leads: HashMap<EcgLeadType, ArrayList<Short>>->
            val firstByte = rawSamples[startOfLine].toInt()
            val secondByte = rawSamples[startOfLine + 1].toInt()
            val thirdByte = rawSamples[startOfLine + 2].toInt()

            val  lowBits = (secondByte shr 4) and 0x0f
            val highBits = (secondByte and 0x0f)

            val firstElem = ((highBits shl 8) or (firstByte and 0xff)).toShort()
            val secondElem = ((lowBits shl 8) or (thirdByte and 0xff)).toShort()

            leads.compute(leadFirst, {
                k: EcgLeadType, v: ArrayList<Short>? ->
                var res: ArrayList<Short>?
                if (v == null)
                    res = ArrayList()
                else
                    res = v
                res.add(firstElem)
                res
            })
            leads.compute(leadSecond, {
                k: EcgLeadType, v: ArrayList<Short>? ->
                var res: ArrayList<Short>?
                if (v == null)
                    res = ArrayList()
                else
                    res = v
                res!!.add(secondElem)
                res
            })

            checksumsReal.compute(leadFirst, {
                k: EcgLeadType, v: Short? ->
                var res: Short = 0;
                if (v != null)
                    res = v
                (res + firstElem).toShort()
            })
            checksumsReal.compute(leadSecond, {
                k: EcgLeadType, v: Short? ->
                var res: Short = 0;
                if (v != null)
                    res = v
                (res + secondElem).toShort()
            })
        }

        readSimultaneousSignals(0, rawSamples, checksumsReal, leads)

        for (lead in leadsPresent) {
            if (checkFirstValues.get(lead)?.equals(leads.get(lead)?.get(0)) == false)
                throw EcgFormatException("ECG data is broken")
        }

        var index = 3;
        while (index < rawSamples.size) {
            readSimultaneousSignals(index, rawSamples, checksumsReal, leads)
            index += 3;
        }

        for (lead in leadsPresent) {
            if (checksumsExpected.get(lead)?.equals(checksumsReal.get(lead)) == false)
                throw EcgFormatException("ECG data is broken")
        }
        return leads
    }

        private fun readEcgRawSignals16(dataFilePath: String, numOfSamples: Int, leadsPresent: LinkedHashSet<EcgLeadType>,
                            checksumsExpected: Map<EcgLeadType, Short>, checkFirstValues: Map<EcgLeadType, Short>): Map<EcgLeadType, ArrayList<Short>> {

        val rawSamples: ByteArray = File(/*"src/main/resources/ecg/" + fileId + ".dat"*/dataFilePath).readBytes()
        val leads = HashMap<EcgLeadType, ArrayList<Short>>()
        val checksumsReal = HashMap<EcgLeadType, Short>()

        var index = 0

        if (rawSamples.size != leadsPresent.size * 2 * numOfSamples)
            throw EcgFormatException("Ecg data is broken")

        val readSimultaneousSignals = {
            startOfLine: Int, rawSamples: ByteArray,
            checksumsReal: HashMap<EcgLeadType, Short>, leads: HashMap<EcgLeadType, ArrayList<Short>> ->
            var index = 0
            for (leadType in leadsPresent) {
                val bb: ByteBuffer = ByteBuffer.allocate(2);
                bb.order(ByteOrder.LITTLE_ENDIAN)
                bb.put(rawSamples[startOfLine + index])
                bb.put(rawSamples[startOfLine + index + 1])
                leads.compute(leadType, {
                    k: EcgLeadType, v: ArrayList<Short>? ->
                    var res: ArrayList<Short>?
                    if (v == null)
                        res = ArrayList()
                    else
                        res = v
                    res.add(bb.getShort(0))
                    res
                })
                checksumsReal.compute(leadType, {
                    k: EcgLeadType, v: Short? ->
                    var res: Short = 0;
                    if (v != null)
                        res = v
                    (res + bb.getShort(0)).toShort()
                })
                index += 2
            }
        }

        readSimultaneousSignals(0, rawSamples, checksumsReal, leads)

        for (lead in leadsPresent) {
            if (checkFirstValues.get(lead)?.equals(leads.get(lead)?.get(0)) == false)
                throw EcgFormatException("ECG data is broken")
        }

        index = leadsPresent.size * 2;
        while (index < rawSamples.size) {
            readSimultaneousSignals(index, rawSamples, checksumsReal, leads)
            index += leadsPresent.size * 2;
        }

        for (lead in leadsPresent) {
            if (checksumsExpected.get(lead)?.equals(checksumsReal.get(lead)) == false)
                throw EcgFormatException("ECG data is broken")
        }

        return leads
    }
}