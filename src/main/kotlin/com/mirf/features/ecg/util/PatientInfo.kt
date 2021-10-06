package com.mirf.features.ecg.util

import java.time.LocalDateTime

data class PatientInfo(val name: String, val age: Int, val sex: String, val dateOfEcg: LocalDateTime)