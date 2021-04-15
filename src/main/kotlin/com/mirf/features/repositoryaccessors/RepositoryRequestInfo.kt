package com.mirf.features.repositoryaccessors

import java.io.Serializable

/**
 * Value type of [RepoAccessorsAttributes.REPOSITORY_REQUEST_INFO]
 */
class RepositoryRequestInfo(val link: String, val requestType: RepositoryRequestType) : Serializable

