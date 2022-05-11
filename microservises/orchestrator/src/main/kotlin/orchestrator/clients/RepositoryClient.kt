package orchestrator.clients

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
import org.springframework.stereotype.Service
import java.io.File

@Service
class RepositoryClient {
//    private val repositoryUri = "http://localhost:8080"
    private val httpclient: CloseableHttpClient = HttpClients.createDefault()

    fun sendFile(filename : String, repositoryUri: String): Boolean {
        val post = HttpPost(repositoryUri)
        val file = File(filename)
        val textFileName = file.name
        val message = "This is a multipart post"
        val builder: MultipartEntityBuilder = MultipartEntityBuilder.create()

        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        builder.addBinaryBody("file", file, ContentType.DEFAULT_BINARY, textFileName)
        builder.addTextBody("text", message, ContentType.DEFAULT_BINARY)

        val entity: HttpEntity = builder.build()
        post.entity = entity
        var response: HttpResponse = httpclient.execute(post)
        var time: Long = 1000
        while (response.statusLine.statusCode != 200) {
            Thread.sleep(time)
            time *= 2

            if (time >= 128000) {
                return false
            }

            response = httpclient.execute(post)
        }

        return true
    }

    fun loadFile(filename : String, repositoryUri: String) {
        val httpGet: HttpUriRequest = HttpGet("$repositoryUri/files/$filename")
        httpclient.execute(httpGet).use { response1 ->
            val entity1: HttpEntity? = response1.entity
            val myFile = File(filename)
            myFile.printWriter().use { out ->
                out.print(EntityUtils.toString(entity1))
            }
        }
        // TODO: check response
    }

    fun ping(repositoryUri: String): Boolean {
        val httpGet: HttpUriRequest = HttpGet("$repositoryUri/ping")

        return try {
            val response = httpclient.execute(httpGet)
            response.statusLine.statusCode == 200
        } catch (e: Exception) {
            false
        }
    }

    fun removeSession(sessionId: String, repositoryUri: String) {
        throw NotImplementedError()
    }
}
