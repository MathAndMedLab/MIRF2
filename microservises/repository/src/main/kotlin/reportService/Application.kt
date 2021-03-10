package reportService

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.MultipartConfigFactory
import org.springframework.boot.web.servlet.ServletComponentScan
import org.springframework.context.annotation.Bean
import org.springframework.util.unit.DataSize
import reportService.clients.OrchestratorClient
import reportService.storage.StorageProperties
import reportService.storage.StorageService
import java.io.File
import java.net.URI
import javax.servlet.MultipartConfigElement

@ServletComponentScan
@SpringBootApplication
@EnableConfigurationProperties(StorageProperties::class)
class Application {

    @Value("\${server.port}")
    private lateinit var serverPort: String

    @Value("\${orchestrator.host}")
    private lateinit var orchestratorHost: String

    @Value("\${orchestrator.port}")
    private lateinit var orchestratorPort: Integer

    @Bean
    fun multipartConfigElement(): MultipartConfigElement? {
        val factory = MultipartConfigFactory()
        factory.setMaxFileSize(DataSize.ofBytes(512000000L))
        factory.setMaxRequestSize(DataSize.ofBytes(512000000L))
        return factory.createMultipartConfig()
    }

    @Bean
    fun init(storageService: StorageService): CommandLineRunner? {

        if (serverPort == "") {
            throw Exception("Server port is not specified.")
        }
//        val file = File("configuration.json")
//        val json = file.readText()
//        val mapper = jacksonObjectMapper()
//        val configuration = mapper.readValue<RepositoryInfo>(json)

//        val appProperties = File("application.properties")
//        val appPropertiesStrings = appProperties.readLines()
//
//        var port: String = ""
//        for (line in appPropertiesStrings) {
//            val sub = line.substring(0, 12)
//            if (sub == "server.port=") {
//                port = line.substring(12)
//            }
//        }
//
//        if (port == "") {
//            throw Exception("Port does not specified.")
//        }

        val orchestratorUri = URI("http", null, orchestratorHost, orchestratorPort.toInt(), null, null, null)

        println("Orchestrator address:" +  orchestratorUri.toString())

        val orchestratorClient = OrchestratorClient(orchestratorUri.toString())
        orchestratorClient.register(serverPort)

        return CommandLineRunner { args: Array<String?>? ->
//            storageService.deleteAll()
//            storageService.init()
        }
    }
}

class RepositoryInfo (
    val orchestratorUri: String) {
    var port: String = ""
}

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}
