package orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import java.nio.file.Paths

@Configuration
class AppConfig {

//    @Bean
//    fun propertySourcesPlaceholderConfigurer(): PropertySourcesPlaceholderConfigurer? {
//        val properties = PropertySourcesPlaceholderConfigurer()
//        val configPath = Paths.get(ClassLoader.getSystemResource("application.properties").toURI())
//        //properties.setLocation(FileSystemResource(configPath))
//        properties.setLocation(FileSystemResource("/home/alexandra/DISK/MIRF2/microservises/orchestrator/src/main/resources/application.properties"))
//        properties.setIgnoreResourceNotFound(false)
//        return properties
//    }


    @Bean
    fun propertyPlaceholderConfigurer(): PropertySourcesPlaceholderConfigurer? {
        return PropertySourcesPlaceholderConfigurer()
    }

}