package orchestrator.data

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

class Pipeline(jsonString: String) {

    private val root: PipelineNode

    init {
        try {
            val mapper = jacksonObjectMapper()
            val nodesRaw: List<PipelineNodeRaw> = mapper.readValue(jsonString)

            val nodes = ArrayList<PipelineNode>()

            for (nodeRaw in nodesRaw) {
                nodes.add(PipelineNode(nodeRaw.blockType, nodeRaw.id))
            }

            root = nodes[0]

            for (i in nodes.indices) {
                for (childIdx in nodesRaw[i].children) {
                    nodes[i].addChild(nodes[childIdx])
                }
            }
        } catch (exception: Exception) {
            throw Exception("invalid pipeline configuration")
        }
    }

    private fun printPipeline(_root: PipelineNode) {
        println("block: ${_root.blockType}, inProgress=${_root.inProgress}, finished=${_root.finished}")
        val children = _root.children
        for (child in children) {
            printPipeline(child)
        }
    }

    fun printPipeline() {
        printPipeline(root)
    }

    private fun setFinished(blockId: Int, _root: PipelineNode, resultFile: String): Boolean {
        if (_root.blockId == blockId) {
            _root.finished = true
            _root.inProgress = false
            _root.resultFile = resultFile
            return true
        }
        val children = _root.children
        if (children.isEmpty()) {
            return false
        }

        for (child in children) {
            if (setFinished(blockId, child, resultFile)) {
                return true
            }
        }
        return false
    }

    fun setFinished(blockId: Int, resultFile: String) {
        setFinished(blockId, root, resultFile)
    }

    private fun setBlockId(pipelineId: Int, blockId: Int, _root: PipelineNode): Boolean {
        if (_root.id == pipelineId) {
            _root.blockId = blockId
            return true
        }

        val children = _root.children
        if (children.isEmpty()) {
            return false
        }

        for (child in children) {
            if (setBlockId(pipelineId, blockId, child)) {
                return true
            }
        }
        return false
    }

    fun setBlockId(pipelineId: Int, blockId: Int) {
        setBlockId(pipelineId, blockId, root)
    }

    private fun setInProgress(blockId: Int, _root: PipelineNode): Boolean {
        if (_root.blockId == blockId) {
            _root.inProgress = true
            return true
        }
        val children = _root.children
        if (children.isEmpty()) {
            return false
        }

        for (child in children) {
            if (setInProgress(blockId, child)) {
                return true
            }
        }
        return false
    }

    fun setInProgress(blockId: Int) {
        setInProgress(blockId, root)
    }

    private fun getAvailable(_root: PipelineNode, listAvailable: ArrayList<PipelineBlock>) {
        if (_root.inProgress) {
            return
        }

        if (!_root.finished) {
            val parents = _root.parents
            val inputFiles: ArrayList<String> = ArrayList()

            for (parent in parents) {
                inputFiles.add(parent.resultFile)
            }

            listAvailable.add(PipelineBlock(_root.id, _root.blockType, inputFiles))
            return
        }

        // root.finished == true
        val children = _root.children
        for (child in children) {
            getAvailable(child, listAvailable)
        }
    }

    fun getAvailable(): List<PipelineBlock> {
        val listAvailable = ArrayList<PipelineBlock>()
        getAvailable(root, listAvailable)
        return listAvailable
    }

    fun getBlocks(level: Int): List<String> = root.getChildren(level)

    private class PipelineNodeRaw(
        val id: Int,
        val blockType: String,
        val children: ArrayList<Int>
    )

    private class PipelineNode(val blockType: String, val id: Int) {
        var finished: Boolean = false
        var inProgress: Boolean = false
        var blockId: Int = -1
        var resultFile: String = ""

        val children: ArrayList<PipelineNode> = ArrayList()
        val parents: ArrayList<PipelineNode> = ArrayList()

        private fun getChildren(level: Int, nodes: ArrayList<PipelineNode>): List<String> {
            if (level < 0) {
                throw IllegalArgumentException("level must be >= 0")
            }
            if (level == 0) {
                val childrenTypes = ArrayList<String>()
                for (node in nodes) {
                    childrenTypes.add(node.blockType)
                }
                return childrenTypes
            }

            val children = ArrayList<PipelineNode>()
            for (node in nodes) {
                for (child in node.children) {
                    children.add(child)
                }
            }
            return getChildren(level - 1, children)
        }

        fun getChildren(level: Int): List<String> {
            if (level < 1) {
                throw IllegalArgumentException("level must be >= 1")
            }
            return getChildren(level, ArrayList())
        }

        fun addChild(node: PipelineNode) {
            node.parents.add(this)
            this.children.add(node)
        }

        fun getNodeType(): BlockType {
            if (children.size == 1 && parents.size == 1) {
                return BlockType.ALGORITHM
            }
            if (children.size <= 1 && parents.size > 1) {
                return BlockType.ACCUMULATOR
            }
            if (children.size > 1 && parents.size <= 1) {
                return BlockType.ELASTIX
            }
            if (children.size > 1 && parents.size > 1) {
                return BlockType.ALGORITHM
            }

            throw Exception("Cannot be reached")
        }
    }
}
