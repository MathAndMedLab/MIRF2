package block.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("storage")
object StorageProperties {
    /**
     * Folder location for storing files
     */
    const val location: String = "upload-dir"

    const val inputFileName: String = "input"
}

