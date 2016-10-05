package ui

enum class BlockType {
    Added,
    Deleted,
    Padding,
    ContextExcluded,
    Matching
}

data class ViewModel(val left: List<BlockModel>, val right: List<BlockModel>) {

}

data class BlockModel(val type: BlockType, val content: String, val children: List<BlockModel>? = null, val padding: BlockModel? = null) {

}

