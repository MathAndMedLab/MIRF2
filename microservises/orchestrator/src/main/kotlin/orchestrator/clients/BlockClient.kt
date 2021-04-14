package orchestrator.clients

import com.fasterxml.jackson.databind.ObjectMapper
import orchestrator.data.Command
import orchestrator.data.NetworkInfo
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.File

@Service
class BlockClient {
    private val httpclient: CloseableHttpClient = HttpClients.createDefault()

    fun sendCommand(blockUri: String, sessionId: String, command: Command,
                    pipelineBlockId: Int, blockId: Int): Boolean {

        val post = HttpPost("$blockUri/process")

        //здесь зипник с именем - номер сессии
        val filesToProcess = command.fileNamesToProcess
        val objectMapper = ObjectMapper()
        val filesToProcessJson = objectMapper.writeValueAsString(filesToProcess)


        val builder: MultipartEntityBuilder = MultipartEntityBuilder.create()

        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        builder.addTextBody("sessionId", sessionId, ContentType.DEFAULT_BINARY)
        builder.addTextBody("filenames", filesToProcessJson, ContentType.DEFAULT_BINARY)
        builder.addTextBody("repositoryUri", command.repositoryUri, ContentType.DEFAULT_BINARY)

        val entity: HttpEntity = builder.build()
        post.entity = entity

        NetworkInfo.setInProgress(pipelineBlockId, blockId, sessionId)

        // TODO: try..catch and sent error if block failed
        val response: HttpResponse = httpclient.execute(post)

        return response.statusLine.statusCode == 200
    }

    fun ping(blockUri: String): Boolean {
        val httpGet: HttpUriRequest = HttpGet("$blockUri/ping")

        return try {
            val response = httpclient.execute(httpGet)
            response.statusLine.statusCode == 200
        } catch (e: Exception) {
            false
        }
    }
}