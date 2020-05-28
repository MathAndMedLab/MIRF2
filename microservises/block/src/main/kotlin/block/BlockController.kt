package block

import block.clients.OrchestratorClient
import block.clients.ProcessingResult
import block.clients.RepositoryClient
import block.execution.Executor
import block.info.BlockInfo
import block.storage.StorageService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import zip.Zipper
import java.io.File
import java.nio.file.Paths

@RestController
class BlockController @Autowired constructor(private val storageService: StorageService,
                                             private val repositoryClient: RepositoryClient) {

    private /*suspend*/ fun processAsync(sessionId: String, filenames: List<String>, repositoryUri: String) {
//        coroutineScope {
//            GlobalScope.launch {
        // async execution
        try {
            val inputObjectFileNames = ArrayList<String>()
            // download and unarchive files
            for (filename in filenames) {
                if (!repositoryClient.loadFile(sessionId, filename, repositoryUri)) {
                    // failed to store
                    Executor.notifyOrchestrator(ProcessingResult.FAILED_TO_STORE, sessionId)
                    return//@launch // TODO: check
                }
                Zipper.unzip(filename, "")

                inputObjectFileNames.add(
                    Paths.get(filename.substring(0, filename.length - 4)).resolve("input").toString()
                )
            }

            val resultFolder = Executor.run(sessionId, inputObjectFileNames)
            val resultFile = "$resultFolder.zip"
            Zipper.zip(resultFolder, resultFile)

            // delete archives
            val resultFolderFile = File(resultFolder)
            resultFolderFile.deleteRecursively()

            for (filename in filenames) {
                val file = File(filename)
                file.deleteRecursively()

                val inputFolder = filename.substring(0, filename.length - 4)
                val inputFolderFile = File(inputFolder)
                inputFolderFile.deleteRecursively()
            }

            if (!repositoryClient.sendFile(sessionId, resultFile, repositoryUri)) {
                // failed to store
                val file = File("$resultFolderFile.zip")
                file.deleteRecursively()
                Executor.notifyOrchestrator(ProcessingResult.FAILED_TO_STORE, sessionId)
                return//@launch // TODO: check
            }
            val file = File("$resultFolderFile.zip")
            file.deleteRecursively()
        } catch (e: Exception) {
            Executor.notifyOrchestrator(ProcessingResult.FAILED_TO_PROCESS, sessionId)
            return
        }
        Executor.notifyOrchestrator(ProcessingResult.SUCCESS, sessionId)
//            }
//        }
    }

    @PostMapping("/process")
            /*suspend*/ fun process(
        @RequestParam("sessionId") sessionId: String,
        @RequestParam("filenames") filenamesJson: String,//List<String>,
        @RequestParam("repositoryUri") repositoryUri: String
    ) /*= coroutineScope*/ {

        val mapper = jacksonObjectMapper()
        val filenames = mapper.readValue<List<String>>(filenamesJson)

        // TODO: make it async
        processAsync(sessionId, filenames, repositoryUri)
    }

    @GetMapping("/ping")
    fun ping() {
    }
}