package block.storage

import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Path
import java.util.stream.Stream

interface StorageService {
    fun init()
    fun createSession(sessionId: String)
    fun store(sessionId: String, file: MultipartFile)
    fun loadAll(): Stream<Path>
    fun load(sessionId: String, filename: String): Path
    fun loadAsResource(sessionId: String, filename: String): Resource
    fun deleteSessionFiles(sessionId: String)
    fun deleteAll()
}

