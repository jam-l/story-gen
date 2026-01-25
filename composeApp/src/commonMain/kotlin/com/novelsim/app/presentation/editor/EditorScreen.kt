package com.novelsim.app.presentation.editor

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.novelsim.app.data.model.*
import com.novelsim.app.presentation.editor.components.*
import com.novelsim.app.presentation.editor.database.DatabaseEditor
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt
import com.novelsim.app.util.PlatformUtils

/**
 * 可视化剧情编辑器屏幕
 */
data class EditorScreen(
    val storyId: String?
) : Screen {
    
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<EditorScreenModel> { parametersOf(storyId) }
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        Scaffold(
            topBar = {
                EditorTopBar(
                    title = uiState.storyTitle,
                    isSaving = uiState.isSaving,
                    isListMode = uiState.isListMode,
                    onBack = {
                        screenModel.saveStory {
                            navigator.pop()
                        }
                    },
                    onSave = { screenModel.saveStory() },
                    onExport = { 
                        val json = screenModel.exportToJson()
                        println(json)
                    },
                    onTitleChange = { screenModel.updateStoryTitle(it) },
                    onToggleViewMode = { screenModel.toggleViewMode() },
                    onOpenDatabase = { screenModel.toggleDatabaseEditor(true) }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { screenModel.toggleAddNodeMenu() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加节点")
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.error != null -> {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        if (uiState.isListMode) {
                            NodeListEditor(
                                nodes = uiState.nodes,
                                enemies = uiState.enemies,
                                onSelectNode = { screenModel.selectNode(it) },
                                onDeleteNode = { screenModel.deleteNode(it) }
                            )
                        } else {
                            NodeEditorCanvas(
                                nodes = uiState.nodes,
                                scale = uiState.canvasScale,
                                offsetX = uiState.canvasOffsetX,
                                offsetY = uiState.canvasOffsetY,
                                onNodeSelect = { screenModel.selectNode(it) },
                                onNodeMove = { id, x, y -> screenModel.moveNode(id, x, y) },
                                onScaleChange = { screenModel.updateCanvasScale(it) },
                                onOffsetChange = { x, y -> screenModel.updateCanvasOffset(x, y) }
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = uiState.showAddNodeMenu,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            AddNodeMenu(
                                onAddNode = { type ->
                                    val centerX = (-uiState.canvasOffsetX + 200f) / uiState.canvasScale
                                    val centerY = (-uiState.canvasOffsetY + 300f) / uiState.canvasScale
                                    screenModel.addNode(type, centerX, centerY)
                                },
                                onDismiss = { screenModel.toggleAddNodeMenu() }
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = uiState.showNodeEditor && uiState.selectedNode != null,
                            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            uiState.selectedNode?.let { node ->
                                NodeEditorPanel(
                                    node = node,
                                    allNodes = uiState.nodes,
                                    characters = uiState.characters,
                                    clues = uiState.clues,
                                    factions = uiState.factions,
                                    locations = uiState.locations,
                                    events = uiState.events,
                                    enemies = uiState.enemies,
                                    onContentChange = { screenModel.updateNodeContent(node.id, it) },
                                    onDelete = { screenModel.deleteNode(node.id) },
                                    onClose = { screenModel.closeNodeEditor() }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 数据库编辑器模态窗口
        if (uiState.showDatabaseEditor) {
            DatabaseEditor(
                onDismissRequest = { screenModel.toggleDatabaseEditor(false) },
                screenModel = screenModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    title: String,
    isSaving: Boolean,
    isListMode: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
    onTitleChange: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenDatabase: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedTitle by remember(title) { mutableStateOf(title) }
    
    TopAppBar(
        title = {
            if (isEditing) {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.6f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isEditing = true }
                ) {
                    Text(
                        text = title.ifEmpty { "未命名故事" },
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑标题",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            // 数据库管理按钮
            IconButton(onClick = onOpenDatabase) {
                Icon(Icons.Default.Settings, contentDescription = "数据库管理")
            }
            
            // 视图切换按钮
            IconButton(onClick = onToggleViewMode) {
                Icon(
                    imageVector = if (isListMode) Icons.Default.Edit else Icons.AutoMirrored.Filled.List,
                    contentDescription = if (isListMode) "切换到画布" else "切换到列表"
                )
            }
            
            if (isEditing) {
                IconButton(onClick = {
                    onTitleChange(editedTitle)
                    isEditing = false
                    onSave()
                }) {
                    Icon(Icons.Default.Check, contentDescription = "确认")
                }
            }
            
            IconButton(onClick = onExport) {
                Icon(Icons.Default.Share, contentDescription = "导出")
            }
            
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onSave) {
                    Icon(Icons.Default.Done, contentDescription = "保存")
                }
            }
        }
    )
}

@Composable
private fun NodeEditorCanvas(
    nodes: List<EditorScreenModel.EditorNode>,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onNodeSelect: (String?) -> Unit,
    onNodeMove: (String, Float, Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Float, Float) -> Unit
) {
    val currentScale by rememberUpdatedState(scale)
    val currentOffsetX by rememberUpdatedState(offsetX)
    val currentOffsetY by rememberUpdatedState(offsetY)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onScaleChange(currentScale * zoom)
                    onOffsetChange(currentOffsetX + pan.x, currentOffsetY + pan.y)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onNodeSelect(null) }
                )
            }
    ) {
        val viewportWidth = constraints.maxWidth
        val viewportHeight = constraints.maxHeight
        
        // Culling: Calculate visible nodes
        // Node width is roughly 160.dp * scale. Let's use a safe margin.
        val nodeWidthPx = 160 * scale * 3 // explicit density conversion omitted for simplicity, using rough factor
        val nodeHeightPx = 200 * scale * 3 
        // 3 is usually density approx on high DPI screens, but accessing local density is better.
        // However, constraints are in pixels. dp to px requires LocalDensity.
        
        // Let's iterate all nodes for Canvas, but filter for Composable
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 50f * scale
            val startX = (offsetX % gridSize)
            val startY = (offsetY % gridSize)
            
            var x = startX
            while (x < size.width) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridSize
            }
            
            var y = startY
            while (y < size.height) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridSize
            }
        }
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            nodes.forEach { editorNode ->
                val fromNode = editorNode.node
                val fromX = fromNode.position.x * scale + offsetX + 80f * scale
                val fromY = fromNode.position.y * scale + offsetY + 40f * scale
                
                fromNode.connections.forEach { connection ->
                    val toNode = nodes.find { it.node.id == connection.targetNodeId }?.node
                    if (toNode != null) {
                        val toX = toNode.position.x * scale + offsetX
                        val toY = toNode.position.y * scale + offsetY + 40f * scale
                        
                        // Simple culling for connection lines:
                        // Only draw if at least one point is somewhat near the viewport
                        // or just rely on Skia clipping which is fast.
                        // Given complexity of Bezier, let's keep drawing all for now,
                        // or just simple bounding box check of the two points.
                        
                        val minX = minOf(fromX, toX)
                        val maxX = maxOf(fromX, toX)
                        val minY = minOf(fromY, toY)
                        val maxY = maxOf(fromY, toY)
                        
                        if (maxX >= 0 && minX <= size.width && maxY >= 0 && minY <= size.height) {
                             val path = Path().apply {
                                moveTo(fromX, fromY)
                                val controlX1 = fromX + 50f * scale
                                val controlX2 = toX - 50f * scale
                                cubicTo(controlX1, fromY, controlX2, toY, toX, toY)
                            }
                            
                            drawPath(
                                path = path,
                                color = Color(0xFF5C6BC0),
                                style = Stroke(width = 2f * scale)
                            )
                            
                            val arrowSize = 8f * scale
                            drawPath(
                                path = Path().apply {
                                    moveTo(toX, toY)
                                    lineTo(toX - arrowSize, toY - arrowSize / 2)
                                    lineTo(toX - arrowSize, toY + arrowSize / 2)
                                    close()
                                },
                                color = Color(0xFF5C6BC0)
                            )
                        }
                    }
                }
            }
        }
        
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        // Optimization: Filter nodes to only render those within the viewport
        val visibleNodes = remember(nodes, scale, offsetX, offsetY, viewportWidth, viewportHeight) {
            nodes.filter { editorNode ->
                val node = editorNode.node
                val nodeX = node.position.x * scale + offsetX
                val nodeY = node.position.y * scale + offsetY
                
                // Approx node size in px
                val w = 180 * scale * density // 160 + padding/border
                val h = 100 * scale * density // Min height approx
                
                // Check intersection with viewport [0, 0, viewportWidth, viewportHeight]
                nodeX + w > 0 && nodeX < viewportWidth && nodeY + h > 0 && nodeY < viewportHeight
            }
        }

        visibleNodes.forEach { editorNode ->
            val node = editorNode.node
            val nodeX = (node.position.x * scale + offsetX).roundToInt()
            val nodeY = (node.position.y * scale + offsetY).roundToInt()
            
            // 使用 key 确保每个节点独立重组
            key(node.id) {
                DraggableNodeCard(
                    node = node,
                    isSelected = editorNode.isSelected,
                    scale = scale,
                    offsetX = nodeX,
                    offsetY = nodeY,
                    onMove = { dx, dy ->
                        val newX = node.position.x + dx / scale
                        val newY = node.position.y + dy / scale
                        onNodeMove(node.id, newX, newY)
                    },
                    onClick = { onNodeSelect(node.id) }
                )
            }
        }
    }
}

/**
 * 可拖拽的节点卡片
 */
@Composable
private fun DraggableNodeCard(
    node: StoryNode,
    isSelected: Boolean,
    scale: Float,
    offsetX: Int,
    offsetY: Int,
    onMove: (Float, Float) -> Unit,
    onClick: () -> Unit
) {
    // 使用相对位移状态，避免与外部 offsetX/Y 状态同步的问题
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnClick by rememberUpdatedState(onClick)
    
    Box(
        modifier = Modifier
            .offset { 
                IntOffset(
                    (offsetX + dragOffset.x).roundToInt(), 
                    (offsetY + dragOffset.y).roundToInt()
                ) 
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { 
                        isDragging = false
                        if (dragOffset != androidx.compose.ui.geometry.Offset.Zero) {
                            currentOnMove(dragOffset.x, dragOffset.y)
                            dragOffset = androidx.compose.ui.geometry.Offset.Zero
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffset = androidx.compose.ui.geometry.Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    }
                )
            }
    ) {
        NodeCard(
            node = node,
            isSelected = isSelected || isDragging,
            scale = scale,
            onClick = currentOnClick
        )
    }
}

@Composable
private fun NodeCard(
    node: StoryNode,
    isSelected: Boolean,
    scale: Float,
    onClick: () -> Unit
) {
    val nodeColor = when (node.type) {
        NodeType.DIALOGUE -> Color(0xFF5C6BC0)
        NodeType.CHOICE -> Color(0xFFFF8F00)
        NodeType.CONDITION -> Color(0xFF00897B)
        NodeType.BATTLE -> Color(0xFFE53935)
        NodeType.ITEM -> Color(0xFF43A047)
        NodeType.VARIABLE -> Color(0xFF8E24AA)
        NodeType.END -> Color(0xFF757575)
    }
    
    // 使用基础图标集中可用的图标
    val nodeIcon = when (node.type) {
        NodeType.DIALOGUE -> Icons.AutoMirrored.Filled.Send
        NodeType.CHOICE -> Icons.Default.List
        NodeType.CONDITION -> Icons.Default.Settings
        NodeType.BATTLE -> Icons.Default.Star
        NodeType.ITEM -> Icons.Default.Favorite
        NodeType.VARIABLE -> Icons.Default.Build
        NodeType.END -> Icons.Default.Check
    }
    
    val contentPreview = when (val content = node.content) {
        is NodeContent.Dialogue -> content.text.take(30)
        is NodeContent.Choice -> content.prompt.take(30)
        is NodeContent.Condition -> content.expression
        is NodeContent.Battle -> "战斗: ${content.enemyId}"
        is NodeContent.ItemAction -> "${content.action}: ${content.itemId}"
        is NodeContent.VariableAction -> "${content.variableName} ${content.operation}"
        is NodeContent.Ending -> content.title
    }
    
    // 计算缩放后的宽度
    val cardWidth = (160 * scale).dp
    
    Card(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape((12 * scale).dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) nodeColor.copy(alpha = 0.15f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 12.dp else 4.dp
        ),
        border = if (isSelected) BorderStroke((2 * scale).dp, nodeColor) else null
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(nodeColor)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = nodeIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = node.type.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = contentPreview,
                modifier = Modifier.padding(8.dp),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AddNodeMenu(
    onAddNode: (NodeType) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "添加节点",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NodeTypeButton("对话", Icons.AutoMirrored.Filled.Send, Color(0xFF5C6BC0)) { 
                    onAddNode(NodeType.DIALOGUE) 
                }
                NodeTypeButton("选择", Icons.Default.List, Color(0xFFFF8F00)) { 
                    onAddNode(NodeType.CHOICE) 
                }
                NodeTypeButton("条件", Icons.Default.Settings, Color(0xFF00897B)) { 
                    onAddNode(NodeType.CONDITION) 
                }
                NodeTypeButton("战斗", Icons.Default.Star, Color(0xFFE53935)) { 
                    onAddNode(NodeType.BATTLE) 
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NodeTypeButton("道具", Icons.Default.Favorite, Color(0xFF43A047)) { 
                    onAddNode(NodeType.ITEM) 
                }
                NodeTypeButton("变量", Icons.Default.Build, Color(0xFF8E24AA)) { 
                    onAddNode(NodeType.VARIABLE) 
                }
                NodeTypeButton("结局", Icons.Default.Check, Color(0xFF757575)) { 
                    onAddNode(NodeType.END) 
                }
                Spacer(modifier = Modifier.width(60.dp))
            }
        }
    }
}

@Composable
private fun NodeTypeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NodeEditorPanel(
    node: StoryNode,
    allNodes: List<EditorScreenModel.EditorNode>,
    characters: List<Character>,
    clues: List<Clue>,
    factions: List<Faction>,
    locations: List<Location> = emptyList(),
    events: List<GameEvent> = emptyList(),
    enemies: List<Enemy> = emptyList(),
    onContentChange: (NodeContent) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "编辑节点",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "类型: ${node.type.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    when (val content = node.content) {
                        is NodeContent.Dialogue -> {
                            DialogueEditor(content, characters, onContentChange)
                        }
                        is NodeContent.Choice -> {
                            ChoiceEditor(
                                content = content,
                                clues = clues,
                                factions = factions,
                                characters = characters,
                                locations = locations,
                                events = events,
                                onContentChange = onContentChange
                            )
                        }
                        is NodeContent.Ending -> {
                            EndingEditor(content, onContentChange)
                        }
                        is NodeContent.Condition -> {
                            ConditionEditor(
                                content = content,
                                availableNodes = allNodes.map { it.node },
                                clues = clues,
                                factions = factions,
                                characters = characters,
                                locations = locations,
                                onContentChange = onContentChange
                            )
                        }
                        is NodeContent.VariableAction -> {
                            VariableEditor(
                                content = content,
                                availableNodes = allNodes.map { it.node },
                                onContentChange = onContentChange
                            )
                        }
                        is NodeContent.Battle -> {
                            BattleEditor(
                                content = content,
                                availableNodes = allNodes.map { it.node },
                                enemies = enemies,
                                onContentChange = onContentChange
                            )
                        }
                        is NodeContent.ItemAction -> {
                            ItemActionEditor(
                                content = content,
                                availableNodes = allNodes.map { it.node },
                                onContentChange = onContentChange
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个节点吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun DialogueEditor(
    content: NodeContent.Dialogue,
    characters: List<Character>,
    onContentChange: (NodeContent) -> Unit
) {
    var speaker by remember(content) { mutableStateOf(content.speaker ?: "") }
    var text by remember(content) { mutableStateOf(content.text) }
    var expanded by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = speaker,
                onValueChange = { 
                    speaker = it
                    onContentChange(content.copy(speaker = it.ifEmpty { null }))
                },
                label = { Text("说话者") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "选择角色")
                    }
                }
            )
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.7f) // Adjust width as needed
            ) {
                DropdownMenuItem(
                    text = { Text("旁白") },
                    onClick = {
                        speaker = "旁白"
                        expanded = false
                        onContentChange(content.copy(speaker = "旁白"))
                    }
                )
                characters.forEach { character ->
                    DropdownMenuItem(
                        text = { Text(character.name) },
                        onClick = {
                            speaker = character.name
                            expanded = false
                            onContentChange(content.copy(
                                speaker = character.name,
                                portrait = character.avatar // Auto-fill avatar if available
                            ))
                        }
                    )
                }
            }
        }
        
        OutlinedTextField(
            value = text,
            onValueChange = { 
                text = it
                onContentChange(content.copy(text = it))
            },
            label = { Text("对话内容") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6
        )
    }
}

@Composable
private fun ChoiceEditor(
    content: NodeContent.Choice,
    clues: List<Clue>,
    factions: List<Faction>,
    characters: List<Character>,
    locations: List<Location>,
    events: List<GameEvent> = emptyList(),
    onContentChange: (NodeContent) -> Unit
) {
    var prompt by remember(content) { mutableStateOf(content.prompt) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = prompt,
            onValueChange = { 
                prompt = it
                onContentChange(content.copy(prompt = it))
            },
            label = { Text("选择提示") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Text(
            text = "选项列表",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        
        content.options.forEachIndexed { index, option ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    var optionText by remember(option) { mutableStateOf(option.text) }
                    
                    OutlinedTextField(
                        value = optionText,
                        onValueChange = { newText ->
                            optionText = newText
                            val newOptions = content.options.toMutableList()
                            newOptions[index] = option.copy(text = newText)
                            onContentChange(content.copy(options = newOptions))
                        },
                        label = { Text("选项 ${index + 1}") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    if (optionText.isNotEmpty()) {
                        EffectsListEditor(
                            effects = option.effects,
                            clues = clues,
                            factions = factions,
                            characters = characters,
                            locations = locations,
                            events = events,
                            onEffectsChange = { newEffects ->
                                val newOptions = content.options.toMutableList()
                                newOptions[index] = option.copy(effects = newEffects)
                                onContentChange(content.copy(options = newOptions))
                            }
                        )
                    }
                }
            }
        }
        
        OutlinedButton(
            onClick = {
                val newOption = ChoiceOption(
                    id = "opt_${PlatformUtils.getCurrentTimeMillis()}",
                    text = "新选项",
                    nextNodeId = ""
                )
                onContentChange(content.copy(options = content.options + newOption))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加选项")
        }
    }
}

@Composable
private fun EndingEditor(
    content: NodeContent.Ending,
    onContentChange: (NodeContent) -> Unit
) {
    var title by remember(content) { mutableStateOf(content.title) }
    var description by remember(content) { mutableStateOf(content.description) }
    var endingType by remember(content) { mutableStateOf(content.endingType) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { 
                title = it
                onContentChange(content.copy(title = it))
            },
            label = { Text("结局标题") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = description,
            onValueChange = { 
                description = it
                onContentChange(content.copy(description = it))
            },
            label = { Text("结局描述") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6
        )
        
        Text(
            text = "结局类型",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EndingType.entries.forEach { type ->
                FilterChip(
                    selected = endingType == type,
                    onClick = {
                        endingType = type
                        onContentChange(content.copy(endingType = type))
                    },
                    label = { Text(type.name) }
                )
            }
        }
    }
}
