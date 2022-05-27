package reportService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import reportService.storage.StorageFileNotFoundException
import reportService.storage.StorageService
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock


@RestController
class FileUploadController @Autowired constructor(private val storageService: StorageService) {

    companion object {
        @JvmField
        val LOCKER = ReentrantLock()
    }

    @PostMapping("/createSession/{sessionId:.+}")
    @Throws(IOException::class)
    fun createSession(@PathVariable sessionId: String) {
        LOCKER.lock()
        try {
            Files.createDirectory(Paths.get(sessionId))
        } finally {
            LOCKER.unlock()
        }
    }

    @PostMapping("/deleteSession/{sessionId:.+}")
    @Throws(IOException::class)
    fun deleteSession(@PathVariable sessionId: String) {
        LOCKER.lock()
        try {
            val file = File(sessionId)
            file.deleteRecursively()
        } finally {
            LOCKER.unlock()
        }
    }

    @PostMapping("/upload")
    fun uploadFile(
        @RequestParam("sessionId") sessionId: String,
        @RequestParam("file") file: MultipartFile
    ) {

        LOCKER.lock()
        try {
            storageService.store(sessionId, file)
        } finally {
            LOCKER.unlock()
        }
    }

    @PostMapping("/uploadTest")
    fun uploadFileTest(
            @RequestParam("file") file: MultipartFile
    ) {

        LOCKER.lock()
        try {
            storageService.store("120", file)
        } finally {
            LOCKER.unlock()
        }
    }

    @GetMapping("/download/{sessionId:.+}&{filename:.+}")
    @ResponseBody
    fun downloadFile(
        @PathVariable("sessionId") sessionId: String,
        @PathVariable("filename") filename: String
    ): ResponseEntity<ByteArray> {
//    ): ResponseEntity<InputStream> {
//    ): ResponseEntity<Resource> {
        println("BLOCK TRIES TO DOWNLOAD FILE $filename, sessionId $sessionId")
        try {
            val file: Resource = storageService.loadAsResource(sessionId, filename)
            val inputStream: InputStream = file.file.inputStream()// .inputStream
//        val buffer = ByteArray(inputStream.available())
            val bytes = inputStream.readBytes()
//        inputStream.read(buffer)
            println("FILE EXTRACTED SUCCESSFULLY, SENDING TO BLOCK")
            return ResponseEntity.ok().header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.filename + "\""
            ).body(bytes)
        } catch (e: Exception) {
            println("FAILED TO SEND FILE $filename TO BLOCK, sessionId $sessionId")
            return ResponseEntity.ok().header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + "ERROR" + "\""
            ).body(ByteArray(0))
        }
    }

    @GetMapping("/ping")
    fun ping() {
    }

    @ExceptionHandler(StorageFileNotFoundException::class)
    fun handleStorageFileNotFound(exc: StorageFileNotFoundException?): ResponseEntity<*> {
        return ResponseEntity.notFound().build<Any>()
    }
}