package reportService.storage

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
//import org.springframework.mock.web.MockMultipartFile
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.annotation.PostConstruct


@Service
class FileSystemStorageService @Autowired constructor(properties: StorageProperties) :
        StorageService {

    private val rootLocation: Path = Paths.get(properties.location)

    @PostConstruct
    override fun init() {
        try {
            println("Created root directory.")
            Files.createDirectories(rootLocation)
        } catch (e: IOException) {
            throw StorageException("Could not initialize storage", e)
        }
    }

    @Override
    override fun store(sessionId: String, file: MultipartFile) {
        if (Files.exists(rootLocation.resolve(sessionId))) {
            throw StorageException("Session $sessionId already exist")
        }

        if (!Files.exists(rootLocation.resolve(sessionId))) {
            println("Created dirs for session")
            Files.createDirectories(Files.createDirectories(rootLocation.resolve(sessionId)))
        }

        val filename = Paths.get(sessionId).resolve(StringUtils.cleanPath(file.originalFilename)).toString()
        try {
            if (file.isEmpty) {
                throw StorageException("Failed to store empty file $filename")
            }
            if (filename.contains("..")) {
                // This is a security check
                throw StorageException(
                        "Cannot store file with relative path outside current directory "
                                + filename)
            }
            file.inputStream.use { inputStream ->
                Files.copy(inputStream,
                        rootLocation.resolve(filename),
                        StandardCopyOption.REPLACE_EXISTING
                )
            }
        } catch (e: IOException) {
            throw StorageException("Failed to store file $filename", e)
        }
    }

    override fun load(sessionId: String, filename: String): Path {
        return rootLocation.resolve(sessionId).resolve(filename)
    }

    @Override
    override fun loadAsResource(sessionId: String, filename: String): Resource {
        return try {
            val file = load(sessionId, filename)
            val resource: Resource = UrlResource(file.toUri())
            println("PATH: " + file.toString())
            println("RES EXISTS: " + resource.exists())
            println("RES READABLE: " + resource.isReadable)
            if (resource.exists() || resource.isReadable) {
                resource
            } else {
                println("res exists: " + resource.exists())
                throw StorageFileNotFoundException(
                        "Could not read file: $filename")
            }
        } catch (e: MalformedURLException) {
            println("Could not read file: $filename")
            println(e.stackTrace)
            throw StorageFileNotFoundException(
                    "Could not read file: $filename", e)
        }
    }

    override fun deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile())
    }
}