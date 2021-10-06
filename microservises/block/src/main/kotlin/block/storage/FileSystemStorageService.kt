package block.storage

import org.apache.tomcat.util.http.fileupload.disk.DiskFileItem
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
//import org.springframework.mock.web.MockMultipartFile
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartFile
import java.io.FileInputStream
import java.io.IOException
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.stream.Stream


@Service
class FileSystemStorageService @Autowired constructor(properties: StorageProperties) :
        StorageService {

    private val rootLocation: Path = Paths.get(properties.location)

    @Override
    override fun store(sessionId: String, file: MultipartFile) {
        val filename = rootLocation.toString() + "/" +
                sessionId + "/" +
                StringUtils.cleanPath(file.originalFilename)
        try {
            if (file.isEmpty) {
                throw StorageException("Failed to store empty file $filename")
            }
            if (filename.contains("..")) {
                // This is a security check
                throw StorageException("Cannot store file with relative path outside current directory $filename")
            }
            file.inputStream.use { inputStream ->
                Files.copy(
                    inputStream,
                    rootLocation.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } catch (e: IOException) {
            throw StorageException("Failed to store file $filename", e)
        }
    }

    override fun loadAll(): Stream<Path> {
        return try {
            Files.walk(rootLocation, 1)
                .filter { path: Path -> path != rootLocation }
                .map { path: Path ->
                    rootLocation.relativize(path)
                }
        } catch (e: IOException) {
            throw StorageException("Failed to read stored files", e)
        }
    }

    override fun load(sessionId: String, filename: String): Path {
        return rootLocation.resolve("$sessionId/$filename")
    }

    @Override
    override fun loadAsResource(sessionId: String, filename: String): Resource {
        return try {
            val file = load(sessionId, filename)
            val resource: Resource = UrlResource(file.toUri())
            if (resource.exists() || resource.isReadable) {
                resource
            } else {
                throw StorageFileNotFoundException(
                    "Could not read file: $filename"
                )
            }
        } catch (e: MalformedURLException) {
            throw StorageFileNotFoundException(
                "Could not read file: $filename", e
            )
        }
    }

    override fun deleteSessionFiles(sessionId: String) {
        FileSystemUtils.deleteRecursively(rootLocation.resolve(sessionId).toFile())
    }

    override fun deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile())
    }

    override fun init() {
        try {
            Files.createDirectories(rootLocation)
        } catch (e: IOException) {
            throw StorageException("Could not initialize storage", e)
        }
    }

    override fun createSession(sessionId: String) {
        try {
            Files.createDirectories(rootLocation.resolve(sessionId))
        } catch (e: IOException) {
            throw StorageException("Could not initialize storage", e)
        }
    }
}