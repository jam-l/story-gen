package com.novelsim.app.presentation.editor

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.novelsim.app.data.model.*
import com.novelsim.app.data.repository.StoryRepository
import com.novelsim.app.data.repository.StoryExporter
import com.novelsim.app.util.PlatformUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 编辑器 ScreenModel
 */
class EditorScreenModel(
    private val storyId: String?,
    private val storyRepository: StoryRepository
) : ScreenModel {
    
    /**
     * 编辑器节点（包含UI状态）
     */
    data class EditorNode(
        val node: StoryNode,
        val isSelected: Boolean = false
    )
    
    /**
     * UI 状态
     */
    data class UiState(
        val isLoading: Boolean = true,
        val story: Story? = null,
        val nodes: List<EditorNode> = emptyList(),
        val selectedNode: StoryNode? = null,
        val showNodeEditor: Boolean = false,
        val showAddNodeMenu: Boolean = false,
        val canvasScale: Float = 1f,
        val canvasOffsetX: Float = 0f,
        val canvasOffsetY: Float = 0f,
        val error: String? = null,
        val isSaving: Boolean = false,
        val storyTitle: String = "新故事",
        val storyDescription: String = "",
        val isListMode: Boolean = false, // 是否为列表模式
        val showDatabaseEditor: Boolean = false, // 是否显示数据库编辑器
        val characters: List<Character> = emptyList(), // 角色列表
        val locations: List<Location> = emptyList(), // 地点列表
        val events: List<GameEvent> = emptyList(), // 事件列表
        val clues: List<Clue> = emptyList(), // 线索列表
        val factions: List<Faction> = emptyList(), // 阵营列表
        val enemies: List<Enemy> = emptyList(), // 怪物列表
        val items: List<Item> = emptyList(), // 道具列表
        val variables: List<String> = emptyList(), // 变量列表 (Key only)
        val skills: List<com.novelsim.app.data.model.Skill> = emptyList(), // 技能列表
        val connectionDisplayMode: ConnectionDisplayMode = ConnectionDisplayMode.ALL
    )

    enum class ConnectionDisplayMode(val label: String) {
        ALL("显示全部连线"),
        SELECTED_OUTGOING("仅显示选中流出"),
        SELECTED_INCOMING("仅显示选中流入")
    }
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true
    }
    
    init {
        if (storyId != null) {
            loadStory()
        } else {
            createNewStory()
        }
    }
    
    /**
     * 加载现有故事
     */
    private fun loadStory() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val story = storyRepository.getStoryById(storyId!!)
            if (story != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    story = story,
                    storyTitle = story.title,
                    storyDescription = story.description,
                    isListMode = story.variables["__editor_view_mode__"] == "list",
                    nodes = story.nodes.values.map { EditorNode(it) }
                )
                // 加载关联数据
                loadCharacters()
                loadLocations()
                loadEvents()
                loadClues()
                loadFactions()
                loadEnemies()
                loadItems()
                loadSkills()
                _uiState.update { it.copy(variables = story.variables.keys.toList()) }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "故事不存在"
                )
            }
        }
    }
    
    /**
     * 创建新故事
     */
    private fun createNewStory() {
        val startNode = StoryNode(
            id = "start",
            type = NodeType.DIALOGUE,
            content = NodeContent.Dialogue(
                speaker = "旁白",
                text = "故事开始...",
                nextNodeId = ""
            ),
            position = NodePosition(200f, 200f)
        )
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            nodes = listOf(EditorNode(startNode))
        )
        
        // 自动保存初始状态
        saveStory()
    }
    
    /**
     * 添加新节点
     */
    fun addNode(type: NodeType, x: Float, y: Float) {
        val nodeId = "node_${PlatformUtils.getCurrentTimeMillis()}"
        
        val content: NodeContent = when (type) {
            NodeType.DIALOGUE -> NodeContent.Dialogue(text = "新对话内容", nextNodeId = "")
            NodeType.CHOICE -> NodeContent.Choice(
                prompt = "请选择",
                options = listOf(
                    ChoiceOption(
                        id = "opt_1",
                        text = "选项1",
                        nextNodeId = ""
                    )
                )
            )
            NodeType.CONDITION -> NodeContent.Condition(
                expression = "variable > 0",
                trueNextNodeId = "",
                falseNextNodeId = ""
            )
            NodeType.BATTLE -> NodeContent.Battle(
                enemyId = _uiState.value.enemies.firstOrNull()?.id ?: "goblin", // 默认选择第一个或哥布林
                winNextNodeId = "",
                loseNextNodeId = ""
            )
            NodeType.ITEM -> NodeContent.ItemAction(
                itemId = "",
                quantity = 1,
                action = ItemActionType.GIVE,
                nextNodeId = ""
            )
            NodeType.VARIABLE -> NodeContent.VariableAction(
                variableName = "variable",
                operation = VariableOperation.SET,
                value = "0",
                nextNodeId = ""
            )
            NodeType.RANDOM -> NodeContent.Random(
                branches = emptyList()
            )
            NodeType.END -> NodeContent.Ending(
                title = "结局",
                description = "故事结束"
            )
        }
        
        // 碰撞偏移逻辑：防止节点完全重叠
        var finalX = x
        var finalY = y
        val existingNodes = _uiState.value.nodes
        
        // 节点大约宽160，高100-200。设置阈值以保证基本不重叠
        val thresholdX = 180f
        val thresholdY = 120f
        
        // 简单的螺旋/步进搜索，尝试找到一个空位
        // 每次向右下移动 40 像素，最多尝试 50 次
        var attempts = 0
        while (attempts < 50) {
            val collision = existingNodes.any {
                kotlin.math.abs(it.node.position.x - finalX) < thresholdX &&
                kotlin.math.abs(it.node.position.y - finalY) < thresholdY
            }
            if (!collision) break
            
            // 碰撞时偏移
            finalX += 40f
            finalY += 40f
            attempts++
        }

        val newNode = StoryNode(
            id = nodeId,
            type = type,
            content = content,
            position = NodePosition(finalX, finalY)
        )
        
        _uiState.value = _uiState.value.copy(
            nodes = _uiState.value.nodes + EditorNode(newNode),
            showAddNodeMenu = false
        )
    }
    
    /**
     * 删除节点
     */
    fun deleteNode(nodeId: String) {
        _uiState.value = _uiState.value.copy(
            nodes = _uiState.value.nodes.filter { it.node.id != nodeId },
            selectedNode = if (_uiState.value.selectedNode?.id == nodeId) null else _uiState.value.selectedNode,
            showNodeEditor = if (_uiState.value.selectedNode?.id == nodeId) false else _uiState.value.showNodeEditor
        )
    }
    
    /**
     * 选择节点
     */
    fun selectNode(nodeId: String?) {
        _uiState.value = _uiState.value.copy(
            nodes = _uiState.value.nodes.map { 
                it.copy(isSelected = it.node.id == nodeId) 
            },
            selectedNode = _uiState.value.nodes.find { it.node.id == nodeId }?.node,
            showNodeEditor = nodeId != null
        )
    }
    
    /**
     * 移动节点
     */
    fun moveNode(nodeId: String, newX: Float, newY: Float) {
        _uiState.value = _uiState.value.copy(
            nodes = _uiState.value.nodes.map { editorNode ->
                if (editorNode.node.id == nodeId) {
                    editorNode.copy(
                        node = editorNode.node.copy(
                            position = NodePosition(newX, newY)
                        )
                    )
                } else {
                    editorNode
                }
            }
        )
    }
    
    /**
     * 更新节点内容
     */
    fun updateNodeContent(nodeId: String, content: NodeContent) {
        val targetIds = when (content) {
            is NodeContent.Dialogue -> listOf(content.nextNodeId)
            is NodeContent.Battle -> listOf(content.winNextNodeId, content.loseNextNodeId)
            is NodeContent.Condition -> listOf(content.trueNextNodeId, content.falseNextNodeId)
            is NodeContent.ItemAction -> listOf(content.nextNodeId)
            is NodeContent.VariableAction -> listOf(content.nextNodeId)
            is NodeContent.Choice -> content.options.map { it.nextNodeId }
            is NodeContent.Random -> content.branches.map { it.nextNodeId }
            is NodeContent.Ending -> emptyList()
        }
        
        val newConnections = targetIds
            .filter { it.isNotEmpty() }
            .distinct()
            .map { Connection(it) }

        _uiState.value = _uiState.value.copy(
            nodes = _uiState.value.nodes.map { editorNode ->
                if (editorNode.node.id == nodeId) {
                    val updatedNode = editorNode.node.copy(
                        content = content,
                        connections = newConnections
                    )
                    editorNode.copy(node = updatedNode)
                } else {
                    editorNode
                }
            }
        )
        // 更新选中节点
        _uiState.value.selectedNode?.let { selected ->
            if (selected.id == nodeId) {
                _uiState.value = _uiState.value.copy(
                    selectedNode = _uiState.value.nodes.find { it.node.id == nodeId }?.node
                )
            }
        }
    }
    
    /**
     * 添加连接
     */
    fun addConnection(fromNodeId: String, toNodeId: String, label: String? = null) {
        _uiState.value = _uiState.value.copy(
            nodes = _uiState.value.nodes.map { editorNode ->
                if (editorNode.node.id == fromNodeId) {
                    val newConnections = editorNode.node.connections + Connection(toNodeId, label)
                    editorNode.copy(node = editorNode.node.copy(connections = newConnections))
                } else {
                    editorNode
                }
            }
        )
    }
    
    /**
     * 移除连接
     */
    fun removeConnection(fromNodeId: String, toNodeId: String) {
        _uiState.value = _uiState.value.copy(
            nodes = _uiState.value.nodes.map { editorNode ->
                if (editorNode.node.id == fromNodeId) {
                    val newConnections = editorNode.node.connections.filter { it.targetNodeId != toNodeId }
                    editorNode.copy(node = editorNode.node.copy(connections = newConnections))
                } else {
                    editorNode
                }
            }
        )
    }
    
    /**
     * 更新画布缩放
     */
    fun updateCanvasScale(scale: Float) {
        _uiState.value = _uiState.value.copy(
            canvasScale = scale.coerceIn(0.3f, 3f)
        )
    }
    
    /**
     * 更新画布偏移
     */
    fun updateCanvasOffset(offsetX: Float, offsetY: Float) {
        _uiState.value = _uiState.value.copy(
            canvasOffsetX = offsetX,
            canvasOffsetY = offsetY
        )
    }
    
    /**
     * 更新故事标题
     */
    fun updateStoryTitle(title: String) {
        _uiState.value = _uiState.value.copy(storyTitle = title)
    }
    
    /**
     * 更新故事描述
     */
    fun updateStoryDescription(description: String) {
        _uiState.value = _uiState.value.copy(storyDescription = description)
    }
    
    /**
     * 显示/隐藏添加节点菜单
     */
    fun toggleAddNodeMenu() {
        _uiState.value = _uiState.value.copy(
            showAddNodeMenu = !_uiState.value.showAddNodeMenu
        )
    }
    
    /**
     * 切换数据库编辑器显示
     */
    fun toggleDatabaseEditor(show: Boolean) {
        _uiState.update { it.copy(showDatabaseEditor = show) }
    }
    
    fun setConnectionDisplayMode(mode: ConnectionDisplayMode) {
        _uiState.update { it.copy(connectionDisplayMode = mode) }
    }

    /**
     * 切换视图模式
     */
    fun toggleViewMode() {
        val newMode = !_uiState.value.isListMode
        _uiState.update { state -> state.copy(isListMode = newMode) }
        
        // 切换模式时自动保存，防止数据丢失
        saveStory()
    }

    /**
     * 关闭节点编辑器
     */
    fun closeNodeEditor() {
        _uiState.value = _uiState.value.copy(
            showNodeEditor = false,
            selectedNode = null,
            nodes = _uiState.value.nodes.map { it.copy(isSelected = false) }
        )
    }
    
    /**
     * 保存故事
     */
    fun saveStory(onSaved: () -> Unit = {}) {
        screenModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            val nodesMap = _uiState.value.nodes.associate { it.node.id to it.node }
            val startNodeId = _uiState.value.nodes.firstOrNull()?.node?.id ?: "start"
            val currentVariables = _uiState.value.story?.variables?.toMutableMap() ?: mutableMapOf()
            
            // 保存编辑器视图模式到变量中
            currentVariables["__editor_view_mode__"] = if (_uiState.value.isListMode) "list" else "canvas"
            
            val story = Story(
                id = _uiState.value.story?.id ?: "story_${PlatformUtils.getCurrentTimeMillis()}",
                title = _uiState.value.storyTitle,
                author = "用户",
                description = _uiState.value.storyDescription,
                startNodeId = startNodeId,
                nodes = nodesMap,
                variables = currentVariables,
                characters = _uiState.value.characters,
                locations = _uiState.value.locations,
                events = _uiState.value.events,
                clues = _uiState.value.clues,
                factions = _uiState.value.factions,
                enemies = _uiState.value.enemies,
                items = _uiState.value.items,
                createdAt = _uiState.value.story?.createdAt ?: PlatformUtils.getCurrentTimeMillis(),
                updatedAt = PlatformUtils.getCurrentTimeMillis()
            )
            
            storyRepository.saveStory(story)
            
            _uiState.update { 
                it.copy(
                    isSaving = false,
                    story = story,
                    variables = currentVariables.keys.toList()
                )
            }
            onSaved()
        }
    }
    
    
    // ============================================================================================
    // 角色管理逻辑
    // ============================================================================================
    
    /**
     * 加载角色列表
     */
    private fun loadCharacters() {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            val characters = storyRepository.getCharacters(storyId)
            _uiState.update { it.copy(characters = characters) }
        }
    }
    
    /**
     * 保存角色
     */
    fun saveCharacter(character: Character) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.saveCharacter(character, storyId)
            loadCharacters()
        }
    }
    
    /**
     * 删除角色
     */
    fun deleteCharacter(characterId: String) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.deleteCharacter(characterId, storyId)
            loadCharacters()
        }
    }
    
    // ============================================================================================
    // 地点管理逻辑
    // ============================================================================================
    
    /**
     * 加载地点列表
     */
    private fun loadLocations() {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            val locations = storyRepository.getLocations(storyId)
            _uiState.update { it.copy(locations = locations) }
        }
    }
    
    /**
     * 保存地点
     */
    fun saveLocation(location: Location) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.saveLocation(location, storyId)
            loadLocations()
        }
    }
    
    /**
     * 删除地点
     */
    fun deleteLocation(locationId: String) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.deleteLocation(locationId, storyId)
            loadLocations()
        }
    }
    
    // ============================================================================================
    // 事件管理逻辑
    // ============================================================================================
    
    /**
     * 加载事件列表
     */
    private fun loadEvents() {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            val events = storyRepository.getEvents(storyId)
            _uiState.update { it.copy(events = events) }
        }
    }
    
    /**
     * 保存事件
     */
    fun saveEvent(event: GameEvent) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.saveEvent(event, storyId)
            loadEvents()
        }
    }
    
    /**
     * 删除事件
     */
    fun deleteEvent(eventId: String) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.deleteEvent(eventId, storyId)
            loadEvents()
        }
    }
    
    // ============================================================================================
    // 线索管理逻辑
    // ============================================================================================
    
    /**
     * 加载线索列表
     */
    private fun loadClues() {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            val clues = storyRepository.getClues(storyId)
            _uiState.update { it.copy(clues = clues) }
        }
    }
    
    /**
     * 保存线索
     */
    fun saveClue(clue: Clue) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.saveClue(clue, storyId)
            loadClues()
        }
    }
    
    /**
     * 删除线索
     */
    fun deleteClue(clueId: String) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.deleteClue(clueId, storyId)
            loadClues()
        }
    }
    
    // ============================================================================================
    // 阵营管理逻辑
    // ============================================================================================
    
    /**
     * 加载阵营列表
     */
    private fun loadFactions() {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            val factions = storyRepository.getFactions(storyId)
            _uiState.update { it.copy(factions = factions) }
        }
    }
    
    /**
     * 保存阵营
     */
    fun saveFaction(faction: Faction) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.saveFaction(faction, storyId)
            loadFactions()
        }
    }
    
    /**
     * 删除阵营
     */
    fun deleteFaction(factionId: String) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.deleteFaction(factionId, storyId)
            loadFactions()
        }
    }
    
    // ============================================================================================
    // 怪物管理逻辑
    // ============================================================================================
    
    /**
     * 加载怪物列表
     */
    private fun loadEnemies() {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            val enemies = storyRepository.getEnemies(storyId)
            _uiState.update { it.copy(enemies = enemies) }
        }
    }
    
    /**
     * 保存怪物
     */
    fun saveEnemy(enemy: Enemy) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.saveEnemy(enemy, storyId)
            loadEnemies()
        }
    }
    
    /**
     * 删除怪物
     */
    fun deleteEnemy(enemyId: String) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.deleteEnemy(enemyId, storyId)
            loadEnemies()
        }
    }

    // ============================================================================================
    // 道具管理逻辑
    // ============================================================================================
    
    /**
     * 加载道具列表
     */
    private fun loadItems() {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            val items = storyRepository.getItems(storyId)
            _uiState.update { it.copy(items = items) }
        }
    }
    
    /**
     * 保存道具
     */
    fun saveItem(item: Item) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.saveItem(item, storyId)
            loadItems()
        }
    }
    
    /**
     * 删除道具
     */
    fun deleteItem(itemId: String) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.deleteItem(itemId, storyId)
            loadItems()
        }
    }

    // ============================================================================================
    // 技能管理逻辑
    // ============================================================================================
    
    /**
     * 加载技能列表
     */
    private fun loadSkills() {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            val skills = storyRepository.getSkills(storyId)
            _uiState.update { it.copy(skills = skills) }
        }
    }
    
    /**
     * 保存技能
     */
    fun saveSkill(skill: com.novelsim.app.data.model.Skill) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.saveSkill(skill, storyId)
            loadSkills()
        }
    }
    
    /**
     * 删除技能
     */
    fun deleteSkill(skillId: String) {
        val storyId = _uiState.value.story?.id ?: return
        screenModelScope.launch {
            storyRepository.deleteSkill(skillId, storyId)
            loadSkills()
        }
    }

    /**
     * 导出为 JSON
     */
    fun exportToJson(): String {
        val nodesMap = _uiState.value.nodes.associate { it.node.id to it.node }
        val startNodeId = _uiState.value.nodes.firstOrNull()?.node?.id ?: "start"
        
        val story = Story(
            id = storyId ?: "story_${PlatformUtils.getCurrentTimeMillis()}",
            title = _uiState.value.storyTitle,
            author = "用户",
            description = _uiState.value.storyDescription,
            startNodeId = startNodeId,
            nodes = nodesMap,
            createdAt = _uiState.value.story?.createdAt ?: PlatformUtils.getCurrentTimeMillis(),
            updatedAt = PlatformUtils.getCurrentTimeMillis()
        )
        
        return StoryExporter.exportToJson(
            story = story,
            characters = _uiState.value.characters,
            locations = _uiState.value.locations,
            events = _uiState.value.events,
            clues = _uiState.value.clues,
            factions = _uiState.value.factions // StoryExporter 暂时还不支持 enemies，需要更新 Exporter
        )
    }

    // ============================================================================================
    // 变量管理逻辑
    // ============================================================================================

    /**
     * 保存变量
     */
    fun saveVariable(name: String, initialValue: String) {
        val currentStory = _uiState.value.story ?: return
        val currentVariables = currentStory.variables.toMutableMap()
        currentVariables[name] = initialValue
        
        val updatedStory = currentStory.copy(variables = currentVariables)
        
        // 立即保存到数据库并更新 UI
        screenModelScope.launch {
             storyRepository.saveStory(updatedStory)
             _uiState.update { 
                 it.copy(
                     story = updatedStory, 
                     variables = updatedStory.variables.keys.toList() 
                 ) 
             }
        }
    }

    /**
     * 删除变量
     */
    fun deleteVariable(name: String) {
        val currentStory = _uiState.value.story ?: return
        val currentVariables = currentStory.variables.toMutableMap()
        if (currentVariables.remove(name) != null) {
            val updatedStory = currentStory.copy(variables = currentVariables)
            
            screenModelScope.launch {
                storyRepository.saveStory(updatedStory)
                _uiState.update { 
                    it.copy(
                        story = updatedStory, 
                        variables = updatedStory.variables.keys.toList() 
                    ) 
                }
            }
        }
    }
    
    // ============================================================================================
    // 随机生成逻辑
    // ============================================================================================
    
    private var randomGenerator = com.novelsim.app.domain.generator.RandomStoryGenerator()
    private var nameProvider: com.novelsim.app.data.source.RandomNameProvider? = null

    private val _nameTemplates = MutableStateFlow<List<com.novelsim.app.data.source.NameTemplate>>(emptyList())
    val nameTemplates: StateFlow<List<com.novelsim.app.data.source.NameTemplate>> = _nameTemplates.asStateFlow()

    init {
        screenModelScope.launch {
             val provider = com.novelsim.app.data.source.RandomNameProvider()
             try {
                 provider.initialize { fileName ->
                     @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
                     novelsimulator.composeapp.generated.resources.Res.readBytes("files/$fileName").decodeToString()
                 }
                 nameProvider = provider
                 randomGenerator = com.novelsim.app.domain.generator.RandomStoryGenerator(nameProvider = provider)
                 _nameTemplates.value = provider.getTemplates()
             } catch (e: Exception) {
                 println("Failed to load name provider in Editor: ${e.message}")
             }
        }
    }
    

     
     fun generateRandomSkill(templateId: String? = null, callback: (Skill) -> Unit) {
         screenModelScope.launch {
             val actualTemplateId = templateId ?: "skill_name_miji"
             val name = nameProvider?.generate(actualTemplateId) ?: "随机招式"
             
             val random = kotlin.random.Random.Default
             val mpCost = random.nextInt(5, 50)
             val isDamage = random.nextBoolean()
             val damage = if (isDamage) random.nextInt(10, 100) else 0
             val heal = if (!isDamage) random.nextInt(10, 80) else 0
             
             callback(Skill(
                 id = "",
                 name = name,
                 description = "随机生成的招式",
                 mpCost = mpCost,
                 damage = damage,
                 heal = heal,
                 animation = "default"
             ))
         }
     }

    fun generateRandomCharacter(templateId: String? = null, count: Int = 1) {
        screenModelScope.launch {
            val rule = com.novelsim.app.domain.generator.RandomStoryGenerator.GenerationRule(
                type = com.novelsim.app.domain.generator.RandomStoryGenerator.EntityType.CHARACTER,
                templateId = templateId,
                count = 1 // Rule count affects batch generation logic inside generator, but here calls createRandomCharacter (single). Pass 1 to avoid confusion or pass count? createRandomCharacter logic relies on rule.templateId. It ignores rule.count usually for single gen.
            )
            repeat(count) { i ->
                val character = randomGenerator.createRandomCharacter(rule, index = _uiState.value.characters.size + 1 + i)
                saveCharacter(character)
            }
        }
    }

    fun generateRandomItem(templateId: String? = null, count: Int = 1) {
        screenModelScope.launch {
            val rule = com.novelsim.app.domain.generator.RandomStoryGenerator.GenerationRule(
                type = com.novelsim.app.domain.generator.RandomStoryGenerator.EntityType.ITEM,
                templateId = templateId,
                count = 1
            )
            repeat(count) { i ->
                val item = randomGenerator.createRandomItem(rule, index = _uiState.value.items.size + 1 + i)
                saveItem(item)
            }
        }
    }
    
    fun generateRandomEnemy(templateId: String? = null, count: Int = 1) {
        screenModelScope.launch {
             val rule = com.novelsim.app.domain.generator.RandomStoryGenerator.GenerationRule(
                type = com.novelsim.app.domain.generator.RandomStoryGenerator.EntityType.ENEMY,
                templateId = templateId,
                count = 1
            )
            repeat(count) { i ->
                val enemy = randomGenerator.createRandomEnemy(rule, index = _uiState.value.enemies.size + 1 + i)
                saveEnemy(enemy)
            }
        }
    }
    
    fun generateRandomVariable(templateId: String? = null, count: Int = 1) {
        val currentStory = _uiState.value.story ?: return
        screenModelScope.launch {
             val rule = com.novelsim.app.domain.generator.RandomStoryGenerator.GenerationRule(
                type = com.novelsim.app.domain.generator.RandomStoryGenerator.EntityType.VARIABLE,
                templateId = templateId,
                count = 1
            )
            repeat(count) { i ->
                val (name, value) = randomGenerator.createRandomVariable(
                    rule, 
                    index = currentStory.variables.size + 1 + i, 
                    existingKeys = currentStory.variables.keys // Note: keys updating during loop? Ideally yes.
                )
                // saveVariable updates state async, so existingKeys might be stale in loop if executed fast. 
                // But createRandomVariable just avoids collisions.
                // saveVariable also updates local map.
                saveVariable(name, value)
            }
        }
    }

    fun generateRandomLocation(templateId: String? = null, count: Int = 1) {
        screenModelScope.launch {
            val rule = com.novelsim.app.domain.generator.RandomStoryGenerator.GenerationRule(
                type = com.novelsim.app.domain.generator.RandomStoryGenerator.EntityType.LOCATION,
                templateId = templateId,
                count = 1
            )
            repeat(count) { i ->
                val location = randomGenerator.createRandomLocation(rule, index = _uiState.value.locations.size + 1 + i)
                saveLocation(location)
            }
        }
    }
}
