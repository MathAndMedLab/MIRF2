package block.execution

import block.storage.StorageProperties
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


class DataCollector (private val blockId: String) {

    fun readObjects(files: List<String>): List<Any> {
        val rootLocation: Path = Paths.get("").toAbsolutePath()

        val result = ArrayList<Any>()
        for (fileName in files) {
            val file = rootLocation.resolve(fileName).toFile()

            val fileInputStream = FileInputStream(file.toString())
            val objectInputStream = ObjectInputStream(fileInputStream)
            val inputObj = objectInputStream.readObject()
            objectInputStream.close()

            result.add(inputObj)
        }

        return result
    }

    fun storeResult(resultObject: Any, sessionId: String): String {
        val folderName: String = "${sessionId}_$blockId"
        val folderPath = Paths.get(folderName)

        if(Files.exists(folderPath)) {
            val dir = File(folderName)
            dir.deleteRecursively()
        }
        Files.createDirectory(Paths.get(folderName))
        // sessionId_blockId/input
        val filename = folderPath.resolve(StorageProperties.inputFileName).toString()

        val fileOutputStream = FileOutputStream(filename)
        val objectOutputStream = ObjectOutputStream(fileOutputStream)
        objectOutputStream.writeObject(resultObject)
        objectOutputStream.flush()
        objectOutputStream.close()

        return folderName
    }
}
