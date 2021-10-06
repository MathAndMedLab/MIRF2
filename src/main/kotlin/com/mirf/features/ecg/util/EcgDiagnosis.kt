package com.mirf.features.ecg.util

import com.mirf.core.data.MirfData
import com.mirf.features.ecg.data.EcgArrhythmiaType
import java.io.Serializable

class EcgDiagnosis(val diagnosis: HashMap<EcgArrhythmiaType, Int>) : MirfData(), Serializable {
}