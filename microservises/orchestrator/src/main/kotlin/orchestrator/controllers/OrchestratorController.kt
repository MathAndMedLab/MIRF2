package orchestrator.controllers

//import org.apache.catalina.servlet4preview.http.HttpServletRequest

import orchestrator.clients.BlockClient
import orchestrator.clients.RepositoryClient
import orchestrator.data.Command
import orchestrator.data.NetworkInfo
import orchestrator.data.Pipeline
import orchestrator.data.ProcessSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URL
import java.util.*
import javax.servlet.http.HttpServletRequest


@RestController
class OrchestratorController @Autowired constructor(
    private val environment: Environment,
    private val repositoryClient: RepositoryClient,
    private val blockClient: BlockClient) {

    @Autowired
    private lateinit var resultSenderHelper: ResultSenderHelper

    //@GetMapping(path = ["/sessionId"], produces = [MediaType.TEXT_PLAIN_VALUE])
    @GetMapping("/sessionId")
    fun getSessionId(): String {
        println("GOT REQUEST FOR SESSION ID")
        return UUID.randomUUID().toString()
    }

    @PostMapping("/process")
    fun process(
        @RequestParam("sessionId") sessionId: String,
        @RequestParam("pipeline") pipelineJson: String
    ) {
        println("RECEIVED REQUEST TO PROCESS PIPELINE")

        val pipeline = Pipeline(pipelineJson)

        val repositoryUri = NetworkInfo.getFreeRepository()

        if (repositoryUri == null) {
            val report: String = "There is no registered repository in the system"
            resultSenderHelper.sendErrorToClient(sessionId, report)
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
                resultSenderHelper.sendErrorToClient(sessionId, report)
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

    @PostMapping("/registeredWebApp")
    fun registerMedWebApp(
//        @RequestParam("port") port: String,
        request: HttpServletRequest
    ): ResponseEntity<String> {
        println("Try to register")
        val address = request.remoteAddr
        val urlSuccess = "http://$address:80/api/mirf/mirfSuccess"
        val urlError = "http://$address:80/api/mirf/mirfError"
        resultSenderHelper.medicalWebAppSuccess = urlSuccess
        resultSenderHelper.medicalWebAppError = urlError
        return ResponseEntity.ok("Saved address successfully")
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
            println("FINISHED PIPELINE")

            // send result to Medical Web App
            val repositoryUri = NetworkInfo.getSessionRepositoryUri(sessionId)
            if (repositoryUri != null) {
                resultSenderHelper.sendResultToClient(sessionId, "${sessionId}_${blockId}.zip", repositoryUri)
            }

            NetworkInfo.removeSession(sessionId)
        }

        for (pipelineBlock in availableBlocks) {
            val block = NetworkInfo.getBlock(pipelineBlock.blockType)

            if (block == null) {
                NetworkInfo.removeSession(sessionId)

                val report: String = "There is no block ${pipelineBlock.blockType} in our system"
                resultSenderHelper.sendErrorToClient(sessionId, report)
                return
            }
            val inputFileNames = pipelineBlock.inputFiles

            val repositoryUri = NetworkInfo.getSessionRepositoryUri(sessionId)
            if (repositoryUri == null) {
                // internal error
                resultSenderHelper.sendErrorToClient(sessionId, "Internal error with repository")
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
            //sessionId.toInt()
        } catch (e: Exception) {
            throw Exception("Invalid block id. blockId=" + blockId)
        }

        val sessionRepositoryUri = NetworkInfo.getSessionRepositoryUri(sessionId)
        // Session will be deleted
        NetworkInfo.blockFinishedError(sessionId, blockIdInt)

        repositoryClient.removeSession(sessionId, sessionRepositoryUri!!)

        resultSenderHelper.sendErrorToClient(sessionId, "Error during block execution")
    }
}