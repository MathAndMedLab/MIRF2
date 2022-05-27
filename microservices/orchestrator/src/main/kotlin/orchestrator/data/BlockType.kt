package orchestrator.data

sealed class BlockType{
    object ALGORITHM : BlockType()
    object ACCUMULATOR : BlockType()
    object ELASTIX : BlockType()
    object MIX : BlockType()
}