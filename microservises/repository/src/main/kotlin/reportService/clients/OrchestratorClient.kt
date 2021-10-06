package reportService.clients

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients

class OrchestratorClient constructor(private val orchestratorUri: String) {

    private val httpclient: CloseableHttpClient =
        HttpClients.createDefault()
    private var blockId: String = ""

    fun register(port: String): Boolean {
        val httpPost = HttpPost("$orchestratorUri/registerRepository")

        val builder: MultipartEntityBuilder =
            MultipartEntityBuilder.create()

        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        builder.addTextBody("port", port, ContentType.DEFAULT_BINARY)

        val entity: HttpEntity = builder.build()
        httpPost.entity = entity

        var response: HttpResponse = httpclient.execute(httpPost)

        var time: Long = 1000
        while (response.statusLine.statusCode != 200) {
            Thread.sleep(time)
            time *= 2

            if (time >= 128000) {
                return false
            }

            response = httpclient.execute(httpPost)
        }

        return true
    }
}