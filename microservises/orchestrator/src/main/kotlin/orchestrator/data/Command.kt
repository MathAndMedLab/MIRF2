package orchestrator.data

data class Command (val fileNamesToProcess: List<String>, val repositoryUri : String)
