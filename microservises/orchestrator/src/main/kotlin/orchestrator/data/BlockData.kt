package orchestrator.data

data class BlockData constructor(
    val id : Int,
    val blockType: String,
    val uri : String,
    val taskLimit : Int = 8,
    var taskLaunched : Int = 0,
    var isAvailable: Boolean = false
)
