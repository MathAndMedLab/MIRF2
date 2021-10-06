package orchestrator.controllers

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ResultSenderHelper {

    private val httpclient: CloseableHttpClient = HttpClients.createDefault()

    @Value("\${medical.web.app.url}")
    private val medicalWebApp: String? = null

    @Value("\${medical.web.app.url.error}")
    var medicalWebAppError: String? = null

    @Value("\${medical.web.app.url.success}")
    var medicalWebAppSuccess: String? = null

    fun sendResultToClient(sessionId: String, filename : String, repositoryUri: String) : Boolean{
        println("START SENDING RESULT TO CLIENT")

        val zipFromRepository = loadResultFromRepository(sessionId, filename, repositoryUri)

        println("LOADED RESULT FROM REPOSITORY")
        val post = HttpPost(medicalWebAppSuccess)
        val builder = MultipartEntityBuilder.create()
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        builder.addTextBody("sessionId", sessionId, ContentType.DEFAULT_BINARY)
        builder.addBinaryBody("file", zipFromRepository, ContentType.DEFAULT_BINARY, filename)

        val entity = builder.build()
        post.entity = entity
        val response: HttpResponse = httpclient.execute(post)

        println("SENT RESULT TO CLIENT: " +  response.statusLine.statusCode)
        return response.statusLine.statusCode == 200
    }

    fun sendErrorToClient(sessionId: String, reason: String) : Boolean{
        println("START SENDING ERROR TO CLIENT")
        val post = HttpPost(medicalWebAppError)
        val builder = MultipartEntityBuilder.create()
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        builder.addTextBody("sessionId", sessionId, ContentType.DEFAULT_BINARY)
        builder.addTextBody("reason", reason, ContentType.DEFAULT_BINARY)
        val entity = builder.build()
        post.entity = entity
        val response: HttpResponse = httpclient.execute(post)
        return response.statusLine.statusCode == 200
    }


    private fun loadResultFromRepository(sessionId: String, filename : String, repositoryUri: String): ByteArray {
        val httpGet: HttpUriRequest =
            HttpGet("$repositoryUri/download/$sessionId&$filename")

        httpclient.execute(httpGet).use { response1 ->
            val entity1: HttpEntity? = response1.entity

            return EntityUtils.toByteArray(entity1)
        }
    }


}