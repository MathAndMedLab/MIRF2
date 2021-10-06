package orchestrator.data

class PipelineBlock (val id: Int, val blockType: String, val inputFiles: List<String>) {
    var executorBlockUri: String? = null
}