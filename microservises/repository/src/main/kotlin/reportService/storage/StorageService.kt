package reportService.storage

import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Path

interface StorageService {
    fun init()
    fun store(sessionId: String, file: MultipartFile)
    fun load(sessionId: String, filename: String): Path
    fun loadAsResource(sessionId: String, filename: String): Resource
    fun deleteAll()
}

