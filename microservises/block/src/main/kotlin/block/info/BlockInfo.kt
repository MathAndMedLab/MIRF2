package block.info

class BlockInfo (
    val blockType: String,
    val inputClassName: String,
    val outputClassName: String,
    val algorithmClassName: String,
    val taskLimit: Int,
    var orchestratorUri: String) {
    var port: String = ""
}
