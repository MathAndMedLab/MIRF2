package block

import block.clients.OrchestratorClient
import block.execution.Executor
import block.info.BlockInfo
import block.storage.StorageProperties
import block.storage.StorageService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import java.io.InputStream
import java.net.URI


@SpringBootApplication
@EnableConfigurationProperties(StorageProperties::class)
open class App {

    @Value("\${server.port}")
    private lateinit var serverPort: String

    @Value("\${block.type}")
    private lateinit var blockType: String

    @Value("\${orchestrator.host}")
    private lateinit var orchestratorHost: String

    @Value("\${orchestrator.port}")
    private lateinit var orchestratorPort: Integer

    @Bean
    open fun init(storageService: StorageService): CommandLineRunner? {
        return CommandLineRunner { args: Array<String?>? ->
            try {
                println(blockType)

                println(serverPort)

                val classLoader = javaClass.classLoader
                val blockConfigurationStream: InputStream? = classLoader.getResourceAsStream(blockType + ".json")

                val jsonBlockConfiguration : String

                if (blockConfigurationStream == null) {
                    throw IllegalArgumentException("Block configuration file not found: " + "blockType" + ".json")
                } else {
                    jsonBlockConfiguration = String(blockConfigurationStream.readBytes())
                    println(jsonBlockConfiguration)
                }

                val mapper = jacksonObjectMapper()
                val configuration = mapper.readValue<BlockInfo>(jsonBlockConfiguration)

                val orchestratorAddress = URI("http", null, orchestratorHost, orchestratorPort.toInt(), null, null, null)

                configuration.orchestratorUri = orchestratorAddress.toString()

                println("Orchestrator address: ${configuration.orchestratorUri}")

//                val appPropertiesStream: InputStream? = classLoader.getResourceAsStream("application.properties")
//
//                val appProperties : String
//
//                if (appPropertiesStream == null) {
//                    throw IllegalArgumentException("Block configuration file not found: " + "application.properties")
//                } else {
//                    appProperties = String(appPropertiesStream.readBytes())
//                    println(appProperties)
//                }
//
//                var port: String = ""
//                val sub = appProperties.substring(0, 12)
//                if (sub == "server.port=") {
//                    port = appProperties.substring(12, 16)
//                }


                if (serverPort == "") {
                    throw Exception("Port does not specified.")
                }

                configuration.port = serverPort

                Executor.init(
                        configuration,
                        OrchestratorClient(
                                configuration.orchestratorUri,
                                configuration.blockType,
                                configuration.taskLimit
                        ),
                        ArrayList<Any>()
                )

            } catch (e: Exception) {
                println("Configuration error")
                throw e
            }
//            storageService.deleteAll()
//            storageService.init()
        }
    }
}

fun main(args: Array<String>) {
    try {
        SpringApplication.run(App::class.java, *args)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
