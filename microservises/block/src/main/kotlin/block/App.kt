package block

import block.clients.OrchestratorClient
import block.execution.Executor
import block.info.BlockInfo
import block.storage.StorageProperties
import block.storage.StorageService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import java.io.InputStream


@SpringBootApplication
@EnableConfigurationProperties(StorageProperties::class)
open class App {
    @Bean
    open fun init(storageService: StorageService): CommandLineRunner? {
        return CommandLineRunner { args: Array<String?>? ->
            try {
                var blockType = System.getProperty("block.type")

                println("System property = " + blockType)

                if (blockType == null) {
                    blockType = "ecgReaderAlg"
                }

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

                val appPropertiesStream: InputStream? = classLoader.getResourceAsStream("application.properties")

                val appProperties : String

                if (appPropertiesStream == null) {
                    throw IllegalArgumentException("Block configuration file not found: " + "application.properties")
                } else {
                    appProperties = String(appPropertiesStream.readBytes())
                    println(appProperties)
                }

                var port: String = ""
                val sub = appProperties.substring(0, 12)
                if (sub == "server.port=") {
                    port = appProperties.substring(12, 16)
                }


                if (port == "") {
                    throw Exception("Port does not specified.")
                }

                configuration.port = port

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
