package com.mirf.features.ecg.data

enum class EcgArrhythmiaType(val fullName: String) {
    NOR("normal"),
    RBB("right bundle branch block"),
    LBB("left bundle branch block"),
    PVC("premature ventricular contraction"),
    PAB("paced"),
    APC("atrial premature contraction"),
    VFW("ventricular flutter wave"),
    VEB("ventricular escape")
}