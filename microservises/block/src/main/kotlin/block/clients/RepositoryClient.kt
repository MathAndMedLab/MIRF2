package block.clients

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
import java.io.FileOutputStream
@Service
class RepositoryClient() {
    private val httpclient: CloseableHttpClient =
        HttpClients.createDefault()

    fun sendFile(sessionId: String, filename : String, repositoryUri: String): Boolean {
        val post = HttpPost("$repositoryUri/upload") // TODO: fix it
        val file = File(filename)
        val textFileName = file.name
        val builder: MultipartEntityBuilder =
            MultipartEntityBuilder.create()

        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        builder.addTextBody("sessionId", sessionId, ContentType.DEFAULT_BINARY)
        builder.addBinaryBody("file", file, ContentType.DEFAULT_BINARY, textFileName)

        val entity: HttpEntity = builder.build()
        post.entity = entity
        var response: HttpResponse = httpclient.execute(post)
        var time: Long = 1000
        while (response.statusLine.statusCode != 200) {
            return false
//            Thread.sleep(time)
//            time *= 2
//
//            if (time >= 128000) {
//                return false
//            }
//
//            response = httpclient.execute(post)
        }

        return true
    }

    fun loadFile(sessionId: String, filename : String, repositoryUri: String): Boolean {
        val httpGet: HttpUriRequest =
            HttpGet("$repositoryUri/download/$sessionId&$filename")

        httpclient.execute(httpGet).use { response1 ->
            val entity1: HttpEntity? = response1.entity

            val bytes = EntityUtils.toByteArray(entity1)

            if (bytes.isEmpty()) {
                return false
            }


            val myFile = File(filename)
            val outStream = FileOutputStream(myFile)
            outStream.write(bytes)
        }
        // TODO: check response
        return true
    }
}
