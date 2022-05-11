package reportService

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

        val orchestratorUri = URI("http", null, orchestratorHost, orchestratorPort.toInt(), null, null, null)

        println("Orchestrator address:$orchestratorUri")

        val orchestratorClient = OrchestratorClient(orchestratorUri.toString())
        orchestratorClient.register(serverPort)

        return CommandLineRunner {
//            storageService.deleteAll()
//            storageService.init()
        }
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}
