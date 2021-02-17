package orchestrator.controllers

import orchestrator.clients.BlockClient
import orchestrator.clients.RepositoryClient
import orchestrator.data.Command
import orchestrator.data.NetworkInfo
import orchestrator.data.Pipeline
import orchestrator.data.ProcessSession
//import org.apache.catalina.servlet4preview.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URL
import javax.servlet.http.HttpServletRequest

@RestController
class OrchestratorController @Autowired constructor(
    private val environment: Environment,
    private val repositoryClient: RepositoryClient,
    private val blockClient: BlockClient) {


    @PostMapping("/process")
    fun process(
        @RequestParam("sessionId") sessionId: String,
        @RequestParam("pipeline") pipelineJson: String
    ) {
        println("Received request from external service")

        val pipeline = Pipeline(pipelineJson)

        val repositoryUri = NetworkInfo.getFreeRepository()

        if (repositoryUri == null) {
            val report: String = "There is no registered repository in the system"
            // TODO: send this report
            return
        }

        val session = ProcessSession(sessionId, pipeline, repositoryUri)
        if (!NetworkInfo.addSession(session)) {
            throw Exception("invalid session id.")
        }

        val availableBlocks = NetworkInfo.getNextPipelineBlocks(sessionId)

        for (pipelineBlock in availableBlocks) {
            val block = NetworkInfo.getBlock(pipelineBlock.blockType)

            if (block == null) {
                NetworkInfo.removeSession(sessionId)

                val report: String = "There is no block ${pipelineBlock.blockType} in our system"
                // TODO: send this report
                return
            }

            val filenames = ArrayList<String>()
            filenames.add("$sessionId.zip")
//            filenames.add("test.shit")

            val command = Command(filenames, repositoryUri)
            try {
                val pipelineBlockId = pipelineBlock.id
                val nextBlockId = block.id
                if (!blockClient.sendCommand(block.uri, sessionId, command, pipelineBlockId, nextBlockId)) {
                    NetworkInfo.removeSession(sessionId)
                    val report: String = "Internal error"
                    // TODO: send this report
                    return
                }
            } catch (e: Exception) {
                NetworkInfo.removeSession(sessionId)
                val report: String = "Internal error"
                // TODO: send this report
                return
            }
        }
    }

    @PostMapping("/registerBlock")
    fun registerBlock(
        @RequestParam("blockType") blockType: String,
        @RequestParam("taskLimit") taskLimit: Int,
        @RequestParam("port") port: String,
        request: HttpServletRequest
    ): ResponseEntity<String> {
        val address = request.remoteAddr
        val url = URL("http://$address:$port")
        return ResponseEntity.ok(NetworkInfo.registerBlock(blockType, url.toString(), taskLimit))
    }

    @PostMapping("/registerRepository")
    fun registerRepository(
        @RequestParam("port") port: String,
        request: HttpServletRequest
    ) {
        val address = request.remoteAddr
        val url = URL("http://$address:$port")
        NetworkInfo.registerRepository(url.toString())
    }

    @PostMapping("/unregisterBlock/{id:.+}")
    fun unregisterBlock(@PathVariable("id") id: String) {
        val idInt: Int

        try {
            idInt = id.toInt()
        } catch (e: Exception) {
            throw Exception("Invalid id. Id=$id")
        }

        NetworkInfo.removeBlock(idInt)
    }

    @PostMapping("/notifySuccess")
    fun blockSuccessFinished(
        @RequestParam("blockId") blockId: String,
        @RequestParam("sessionId") sessionId: String
    ) {
        val blockIdInt: Int

        try {
            blockIdInt = blockId.toInt()
        } catch (e: Exception) {
            throw Exception("Invalid block id.")
        }

        if(!NetworkInfo.blockFinishedSuccess(sessionId, blockIdInt)) {
            return
        }

        val availableBlocks = NetworkInfo.getNextPipelineBlocks(sessionId)

        if (availableBlocks.isEmpty()) {
            NetworkInfo.removeSession(sessionId)
        }

        for (pipelineBlock in availableBlocks) {
            val block = NetworkInfo.getBlock(pipelineBlock.blockType)

            if (block == null) {
                NetworkInfo.removeSession(sessionId)

                val report: String = "There is no block ${pipelineBlock.blockType} in our system"
                // TODO: send this report
                return
            }
            val inputFileNames = pipelineBlock.inputFiles

            val repositoryUri = NetworkInfo.getSessionRepositoryUri(sessionId)
            if (repositoryUri == null) {
                // internal error
                // TODO: send error to client
                NetworkInfo.removeSession(sessionId)
                return
            }

            val nextBlockId = block.id
            val pipelineBlockId = pipelineBlock.id
            val command = Command(inputFileNames, repositoryUri)
            blockClient.sendCommand(block.uri, sessionId, command, pipelineBlockId, nextBlockId)
        }
        // TODO: next task and add changes to pipeline
        // TODO: add address for sending results and error reports
//        if (!NetworkInfo.removeSession(sessionId)) {
//            throw Exception("Invalid session id.")
//        }
    }

    @PostMapping("/notifyError")
    fun blockFinishedWithError(
        @RequestParam("blockId") blockId: String,
        @RequestParam("sessionId") sessionId: String
    ) {
        val blockIdInt: Int

        try {
            blockIdInt = blockId.toInt()
            sessionId.toInt()
        } catch (e: Exception) {
            throw Exception("Invalid block or session id.")
        }

        val sessionRepositoryUri = NetworkInfo.getSessionRepositoryUri(sessionId)
        // Session will be deleted
        NetworkInfo.blockFinishedError(sessionId, blockIdInt)

        repositoryClient.removeSession(sessionId, sessionRepositoryUri!!)

        // TODO: send error report to client
    }
}