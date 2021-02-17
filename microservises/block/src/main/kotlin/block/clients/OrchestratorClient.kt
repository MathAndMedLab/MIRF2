package block.clients

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.springframework.web.bind.annotation.RequestParam

sealed class ProcessingResult{
    object SUCCESS : ProcessingResult()
    @Suppress("ClassName")
    object FAILED_TO_PROCESS : ProcessingResult()
    @Suppress("ClassName")
    object FAILED_TO_STORE : ProcessingResult()
}


//first start orchestrator
class OrchestratorClient constructor(private val orchestratorUri: String,
                                        private val blockType: String,
                                        private val taskLimit: Int) {

    private val httpclient: CloseableHttpClient = HttpClients.createDefault()
    private var blockId: String = ""

    fun register(port: String): String? {
        val httpPost = HttpPost("$orchestratorUri/registerBlock")

        val builder: MultipartEntityBuilder = MultipartEntityBuilder.create()

        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        builder.addTextBody("blockType", blockType, ContentType.DEFAULT_BINARY)
        builder.addTextBody("taskLimit", taskLimit.toString(), ContentType.DEFAULT_BINARY)
        builder.addTextBody("port", port, ContentType.DEFAULT_BINARY)

        val entity: HttpEntity = builder.build()
        httpPost.entity = entity

        var response: HttpResponse = httpclient.execute(httpPost)

        var time: Long = 1000
        while (response.statusLine.statusCode != 200) {
            Thread.sleep(time)
            time *= 2

            if (time >= 128000) {
                return null
            }

            response = httpclient.execute(httpPost)
        }

        val httpEntity = response.entity
        blockId = EntityUtils.toString(httpEntity)
        return blockId
    }

    fun notify(result: ProcessingResult, sessionId: String, errorMessage: String = "") {
        if (result == ProcessingResult.SUCCESS) {
            val httpPost = HttpPost("$orchestratorUri/notifySuccess")

            val builder: MultipartEntityBuilder = MultipartEntityBuilder.create()
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            builder.addTextBody("blockId", blockId, ContentType.DEFAULT_BINARY)
            builder.addTextBody("sessionId", sessionId, ContentType.DEFAULT_BINARY)

            val entity: HttpEntity = builder.build()
            httpPost.entity = entity

            httpclient.execute(httpPost)
            return
        } else if (result == ProcessingResult.FAILED_TO_PROCESS || result == ProcessingResult.FAILED_TO_STORE) {
            val httpPost = HttpPost("$orchestratorUri/notifyError")

            val builder: MultipartEntityBuilder = MultipartEntityBuilder.create()
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            builder.addTextBody("blockId", blockId, ContentType.DEFAULT_BINARY)
            builder.addTextBody("sessionId", sessionId, ContentType.DEFAULT_BINARY)

            val entity: HttpEntity = builder.build()
            httpPost.entity = entity

            val response = httpclient.execute(httpPost)
            return
        }
    }
}
