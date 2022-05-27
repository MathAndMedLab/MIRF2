package com.mirf.core.repository

import java.io.Serializable

/**
 * Info about [RepositoryCommander]. Used for reports generation
 */
data class RepositoryInfo(val repositoryName: String, val username: String) : Serializable {

    fun copy(): RepositoryInfo {
        return RepositoryInfo(repositoryName, username)
    }
}
