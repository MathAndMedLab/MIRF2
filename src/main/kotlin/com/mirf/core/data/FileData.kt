package com.mirf.core.data

import java.io.Serializable

/**
 * [Data] that represents single file
 */
class FileData(val fileBytes: ByteArray, val name: String, val extension: String) : MirfData(), Serializable
