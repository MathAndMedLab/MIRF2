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
import java.io.File


@SpringBootApplication
@EnableConfigurationProperties(StorageProperties::class)
open class App {
    @Bean
    open fun init(storageService: StorageService): CommandLineRunner? {
        return CommandLineRunner { args: Array<String?>? ->
            try {
                val file = File("configuration.json")
                val json = file.readText()
                val mapper = jacksonObjectMapper()
                val configuration = mapper.readValue<BlockInfo>(json)

                val appProperties = File("application.properties")
                val appPropertiesStrings = appProperties.readLines()

                var port: String = ""
                for (line in appPropertiesStrings) {
                    val sub = line.substring(0, 12)
                    if (sub == "server.port=") {
                        port = line.substring(12)
                    }
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
                print("Configuration error")
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
